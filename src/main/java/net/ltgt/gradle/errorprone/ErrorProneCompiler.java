package net.ltgt.gradle.errorprone;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JdkJavaCompiler;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.Factory;
import org.gradle.internal.UncheckedException;
import org.gradle.language.base.internal.compile.Compiler;

public class ErrorProneCompiler implements Compiler<JavaCompileSpec> {
  private static final Logger LOGGER = Logging.getLogger(ErrorProneCompiler.class);

  private final Configuration errorprone;

  public ErrorProneCompiler(Configuration errorprone) {
    this.errorprone = errorprone;
  }

  @Override
  public WorkResult execute(JavaCompileSpec spec) {
    LOGGER.info("Compiling with error-prone compiler");

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
    try (SelfFirstClassLoader cl =
        new SelfFirstClassLoader(urls, JdkJavaCompiler.class.getClassLoader())) {
      Thread.currentThread().setContextClassLoader(cl);

      // Those classes only exist starting with Gradle 4.2, for proper JDK 9 support
      cl.defineClassIfExists(
          "org.gradle.api.internal.tasks.compile.reflect.SourcepathIgnoringInvocationHandler");
      cl.defineClassIfExists(
          "org.gradle.api.internal.tasks.compile.reflect.SourcepathIgnoringProxy");

      Class<JdkJavaCompiler> compilerClass = cl.defineClass(JdkJavaCompiler.class);

      Compiler<JavaCompileSpec> compiler =
          compilerClass
              .getConstructor(Factory.class)
              .newInstance(
                  (Factory<?>)
                      () -> {
                        try {
                          return cl.loadClass("com.google.errorprone.ErrorProneJavaCompiler")
                              .getConstructor()
                              .newInstance();
                        } catch (Exception e) {
                          throw UncheckedException.throwAsUncheckedException(e);
                        }
                      });
      return compiler.execute(spec);
    } catch (InvocationTargetException e) {
      throw UncheckedException.unwrapAndRethrow(e);
    } catch (Exception e) {
      throw UncheckedException.throwAsUncheckedException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
  }
}
