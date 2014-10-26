package net.ltgt.gradle.errorprone;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.internal.tasks.compile.NormalizingJavaCompiler;
import org.gradle.language.base.internal.compile.CompileSpec;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.runtime.jvm.internal.toolchain.JavaToolChainInternal;

public class ErrorProneToolChain implements JavaToolChainInternal {

  public static ErrorProneToolChain create(Project project) {
    return new ErrorProneToolChain(project.getConfigurations().getByName("errorprone"));
  }

  private final Configuration configuration;

  public ErrorProneToolChain(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public String getDisplayName() {
    return String.format("Error-prone; current JDK (%s)", JavaVersion.current());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends CompileSpec> Compiler<T> newCompiler(T spec) {
    if (spec instanceof JavaCompileSpec) {
      return (Compiler<T>) new NormalizingJavaCompiler(new ErrorProneCompiler(configuration));
    }
    throw new IllegalArgumentException(String.format("Don't know how to compile using spec of type %s.", spec.getClass().getSimpleName()));
  }
}
