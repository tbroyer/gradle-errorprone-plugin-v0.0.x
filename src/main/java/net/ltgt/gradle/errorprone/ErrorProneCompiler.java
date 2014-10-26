package net.ltgt.gradle.errorprone;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.internal.tasks.compile.CompilationFailedException;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JavaCompilerArgumentsBuilder;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.jvm.Jvm;
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

    List<String> args = new JavaCompilerArgumentsBuilder(spec).includeSourceFiles(true).build();

    List<URL> urls = new ArrayList<URL>();
    try {
      urls.add(Jvm.current().getToolsJar().toURI().toURL());
      for (File f : errorprone) {
        urls.add(f.toURI().toURL());
      }
    } catch (MalformedURLException mue) {
      throw new RuntimeException(mue.getMessage(), mue);
    }

    URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), null);

    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    int result = 0;
    try {
      Thread.currentThread().setContextClassLoader(cl);

      Class<?> builderClass = cl.loadClass("com.google.errorprone.ErrorProneCompiler$Builder");
      Object compilerBuilder = builderClass.newInstance();
      Object compiler = builderClass.getMethod("build").invoke(compilerBuilder);
      result = (Integer) compiler.getClass().getMethod("compile", String[].class).invoke(compiler, (Object) args.toArray(new String[args.size()]));
    } catch (Exception e) {
      RuntimeException re = (e instanceof RuntimeException)
          ? (RuntimeException) e
          : new RuntimeException(e.getMessage(), e);
      throw re;
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }

    if (result != 0) {
      throw new CompilationFailedException(result);
    }

    return new SimpleWorkResult(true);
  }
}
