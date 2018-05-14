package net.ltgt.gradle.errorprone;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.tasks.compile.CompilationFailedException;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JavaCompilerArgumentsBuilder;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.UncheckedException;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.util.GradleVersion;

public class ErrorProneCompiler implements Compiler<JavaCompileSpec> {
  private static final Logger LOGGER = Logging.getLogger(ErrorProneCompiler.class);

  // Gradle 4.2 introduced WorkResults, and made SimpleWorkResult nag users.
  @SuppressWarnings("deprecation")
  private static final WorkResult DID_WORK =
      GradleVersion.current().compareTo(GradleVersion.version("4.2")) >= 0
          ? org.gradle.api.tasks.WorkResults.didWork(true)
          : new org.gradle.api.internal.tasks.SimpleWorkResult(true);

  private final Configuration errorprone;

  public ErrorProneCompiler(Configuration errorprone) {
    this.errorprone = errorprone;
  }

  @Override
  public WorkResult execute(JavaCompileSpec spec) {
    LOGGER.info("Compiling with error-prone compiler");

    List<String> args = new JavaCompilerArgumentsBuilder(spec).includeSourceFiles(true).build();

    Set<URI> jarUris = errorprone.getFiles().stream().map(File::toURI).collect(Collectors.toSet());
    ErrorProneJars errorProneJars = new ErrorProneJars(jarUris);

    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    SelfFirstClassLoader cl = SelfFirstClassLoader.getInstance(errorProneJars);

    int exitCode;
    try {
      Thread.currentThread().setContextClassLoader(cl);

      Class<?> builderClass = cl.loadClass("com.google.errorprone.ErrorProneCompiler$Builder");
      Object compilerBuilder = builderClass.getConstructor().newInstance();
      Object compiler = builderClass.getMethod("build").invoke(compilerBuilder);
      Object result =
          compiler
              .getClass()
              .getMethod("compile", String[].class)
              .invoke(compiler, (Object) args.toArray(new String[args.size()]));
      exitCode = result.getClass().getField("exitCode").getInt(result);
    } catch (Exception e) {
      throw UncheckedException.throwAsUncheckedException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
      cl.decrementRefCount();
    }
    if (exitCode != 0) {
      throw new CompilationFailedException(exitCode);
    }

    return DID_WORK;
  }

  static class SelfFirstClassLoader extends URLClassLoader {

    private static final ClassLoader PLATFORM_CLASSLOADER;
    private static final ClassLoader PARENT_CLASSLOADER;

    static {
      ClassLoader platformClassloader, parentClassloader;
      try {
        platformClassloader =
            ClassLoader.class.cast(
                ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null));
        parentClassloader = platformClassloader;
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        assert !JavaVersion.current().isJava9Compatible();
        platformClassloader = new ClassLoader(null) {};
        parentClassloader = null;
      }
      PLATFORM_CLASSLOADER = platformClassloader;
      PARENT_CLASSLOADER = parentClassloader;
    }

    /**
     * Cache ClassLoader to allow JVM properly JIT loaded classes and reuse optimizations after
     * warm-up.
     */
    private static volatile SelfFirstClassLoader INSTANCE;

    private static final Object LOCK = new Object();

    static {
      // Both SelfFirstClassLoader and URLClassLoader comply with parallelCapable requirements.
      registerAsParallelCapable();
    }

    /**
     * Returns an instance of ClassLoader for required set of jars, caller is responsible for a call
     * to {@link #decrementRefCount()} when returned ClassLoader is no longer used.
     */
    static SelfFirstClassLoader getInstance(final ErrorProneJars errorProneJars) {
      SelfFirstClassLoader instance = INSTANCE;

      if (!canReuseClassLoader(instance, errorProneJars)) {
        synchronized (LOCK) {
          instance = INSTANCE;

          if (!canReuseClassLoader(instance, errorProneJars)) {
            if (instance != null) {
              instance.decrementRefCount(); // Indicate release of static reference.
            }

            instance = INSTANCE = new SelfFirstClassLoader(errorProneJars);
            instance.incrementRefCount(); // Indicate new static reference.
          }
        }
      }

      instance.incrementRefCount(); // Indicate new reference returned to the caller.
      return instance;
    }

    /**
     * Keeps track of references to the ClassLoader to close it as soon as we know it's safe instead
     * of waiting for GC.
     */
    private final AtomicInteger refCount = new AtomicInteger();

    private final ErrorProneJars errorProneJars;

    private SelfFirstClassLoader(ErrorProneJars errorProneJars) {
      super(
          errorProneJars
              .jars
              .keySet()
              .stream()
              .map(
                  uri -> {
                    try {
                      return uri.toURL();
                    } catch (MalformedURLException e) {
                      throw UncheckedException.throwAsUncheckedException(e);
                    }
                  })
              .toArray(URL[]::new),
          PARENT_CLASSLOADER);
      this.errorProneJars = errorProneJars;
    }

    private void incrementRefCount() {
      refCount.incrementAndGet();
    }

    void decrementRefCount() {
      final int refCountLocal = refCount.decrementAndGet();

      if (refCountLocal == 0) {
        try {
          close();
        } catch (IOException e) {
          throw UncheckedException.throwAsUncheckedException(e);
        }
      } else if (refCountLocal < 0) {
        throw new IllegalStateException("refCount is below zero.");
      }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(name)) {
        Class<?> cls = findLoadedClass(name);
        if (cls == null) {
          try {
            cls = findClass(name);
          } catch (ClassNotFoundException cnfe) {
            // ignore, fallback to bootstrap classloader
          }
          if (cls == null) {
            cls = PLATFORM_CLASSLOADER.loadClass(name);
          }
        }
        if (resolve) {
          resolveClass(cls);
        }
        return cls;
      }
    }

    @Override
    public URL getResource(String name) {
      URL resource = findResource(name);
      if (resource == null) {
        resource = PLATFORM_CLASSLOADER.getResource(name);
      }
      return resource;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
      Enumeration<URL> selfResources = findResources(name);
      Enumeration<URL> bootstrapResources = PLATFORM_CLASSLOADER.getResources(name);
      if (!selfResources.hasMoreElements()) {
        return bootstrapResources;
      }
      if (!bootstrapResources.hasMoreElements()) {
        return selfResources;
      }
      ArrayList<URL> resources = Collections.list(selfResources);
      resources.addAll(Collections.list(bootstrapResources));
      return Collections.enumeration(resources);
    }

    // XXX: we know URLClassLoader#getResourceAsStream calls getResource, so we don't have to
    // override it here.

    private static boolean canReuseClassLoader(
        SelfFirstClassLoader cachedInstance, ErrorProneJars newErrorProneJars) {
      return cachedInstance != null && cachedInstance.errorProneJars.equals(newErrorProneJars);
    }
  }

  static final class ErrorProneJars {
    final Map<URI, JarIdentity> jars;

    ErrorProneJars(Set<URI> jars) {
      this.jars =
          jars.stream()
              .map(jarUri -> new HashMap.SimpleEntry<>(jarUri, new JarIdentity(jarUri)))
              .collect(
                  Collectors.toMap(
                      AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ErrorProneJars that = (ErrorProneJars) o;
      return Objects.equals(jars, that.jars);
    }

    @Override
    public int hashCode() {
      return Objects.hash(jars);
    }

    private static final class JarIdentity {
      private final long lastModified;
      private final long lengthBytes;

      JarIdentity(URI jarUri) {
        final File jar = new File(jarUri);
        this.lastModified = jar.lastModified();
        this.lengthBytes = jar.length();
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JarIdentity that = (JarIdentity) o;
        return lastModified == that.lastModified && lengthBytes == that.lengthBytes;
      }

      @Override
      public int hashCode() {
        return Objects.hash(lastModified, lengthBytes);
      }
    }
  }
}
