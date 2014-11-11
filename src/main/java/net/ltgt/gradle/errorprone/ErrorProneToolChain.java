package net.ltgt.gradle.errorprone;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.internal.tasks.compile.NormalizingJavaCompiler;
import org.gradle.jvm.internal.toolchain.JavaToolChainInternal;
import org.gradle.jvm.platform.JavaPlatform;
import org.gradle.language.base.internal.compile.CompileSpec;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.platform.base.internal.toolchain.ToolProvider;
import org.gradle.util.TreeVisitor;

public class ErrorProneToolChain implements JavaToolChainInternal {

  public static ErrorProneToolChain create(Project project) {
    return new ErrorProneToolChain(project.getConfigurations().getByName("errorprone"));
  }

  private final Configuration configuration;
  private final JavaVersion javaVersion = JavaVersion.current();

  public ErrorProneToolChain(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public String getName() {
    return String.format("ErrorProneJDK%s", javaVersion);
  }

  @Override
  public String getDisplayName() {
    return String.format("Error-prone; JDK %s (%s)", javaVersion.getMajorVersion(), javaVersion);
  }

  @Override
  public ToolProvider select(JavaPlatform targetPlatform) {
    if (targetPlatform != null && targetPlatform.getTargetCompatibility().compareTo(javaVersion) > 0) {
      return new UnavailableToolProvider(targetPlatform);
    }
    return new JavaToolProvider();
  }

  private class JavaToolProvider implements ToolProvider {
    @SuppressWarnings("unchecked")
    public <T extends CompileSpec> Compiler<T> newCompiler(T spec) {
      if (spec instanceof JavaCompileSpec) {
        return (Compiler<T>) new NormalizingJavaCompiler(new ErrorProneCompiler(configuration));
      }
      throw new IllegalArgumentException(String.format("Don't know how to compile using spec of type %s.", spec.getClass().getSimpleName()));
    }

    public boolean isAvailable() {
      return true;
    }

    public void explain(TreeVisitor<? super String> visitor) {
    }
  }

  private class UnavailableToolProvider implements ToolProvider {
    private final JavaPlatform targetPlatform;

    private UnavailableToolProvider(JavaPlatform targetPlatform) {
      this.targetPlatform = targetPlatform;
    }

    public <T extends CompileSpec> Compiler<T> newCompiler(T spec) {
      throw new IllegalArgumentException(getMessage());
    }

    public boolean isAvailable() {
      return false;
    }

    public void explain(TreeVisitor<? super String> visitor) {
      visitor.node(getMessage());
    }

    private String getMessage() {
      return String.format("Could not target platform: '%s' using tool chain: '%s'.", targetPlatform.getDisplayName(), ErrorProneToolChain.this.getDisplayName());
    }
  }
}
