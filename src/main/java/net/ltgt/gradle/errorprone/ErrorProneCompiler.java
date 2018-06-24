package net.ltgt.gradle.errorprone;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.annotation.Nullable;
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

    URL[] urls =
        errorprone
            .getFiles()
            .stream()
            .map(
                file -> {
                  try {
                    return file.toURI().toURL();
                  } catch (MalformedURLException e) {
                    throw UncheckedException.throwAsUncheckedException(e);
                  }
                })
            .toArray(URL[]::new);

    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    int exitCode;
    try (URLClassLoader cl = new SelfFirstClassLoader(urls)) {
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
    }
    if (exitCode != 0) {
      throw new CompilationFailedException(exitCode);
    }

    return DID_WORK;
  }

  private static class SelfFirstClassLoader extends URLClassLoader {

    private static final ClassLoader PLATFORM_CLASSLOADER;
    @Nullable private static final ClassLoader PARENT_CLASSLOADER;

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

    public SelfFirstClassLoader(URL[] urls) {
      super(urls, PARENT_CLASSLOADER);
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
  }
}
