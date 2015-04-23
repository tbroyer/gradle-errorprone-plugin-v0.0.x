package net.ltgt.gradle.errorprone;

import java.util.Collections;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.platform.JavaPlatform;
import org.gradle.language.base.internal.compile.CompileSpec;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.platform.base.Platform;
import org.gradle.platform.base.internal.toolchain.DefaultResolvedCompiler;
import org.gradle.platform.base.internal.toolchain.DefaultResolvedTool;
import org.gradle.platform.base.internal.toolchain.ResolvedTool;
import org.gradle.platform.base.internal.toolchain.ToolResolver;
import org.gradle.platform.base.internal.toolchain.ToolSearchResult;

public class ErrorPronePlugin implements Plugin<Project> {
  @Override
  public void apply(final Project project) {
    project.apply(Collections.singletonMap("plugin", ErrorProneBasePlugin.class));

    final ErrorProneToolChain toolChain = ErrorProneToolChain.create(project);
    final Action<JavaCompile> action = new Action<JavaCompile>() {
      @Override
      public void execute(JavaCompile task) {
        final ToolResolver oldToolResolver = task.getToolResolver();
        task.setToolResolver(new ToolResolver() {
          @Override
          public <P extends Platform> ToolSearchResult checkToolAvailability(P requirement) {
            if (requirement instanceof JavaPlatform) {
              return toolChain.select((JavaPlatform) requirement);
            }
            return oldToolResolver.checkToolAvailability(requirement);
          }

          @Override
          public <T, P extends Platform> ResolvedTool<T> resolve(Class<T> toolType, P requirement) {
            if (requirement instanceof JavaPlatform) {
              return new DefaultResolvedTool<T>(toolChain.select((JavaPlatform) requirement), toolType);
            }
            return oldToolResolver.resolve(toolType, requirement);
          }

          @Override
          public <C extends CompileSpec, P extends Platform> ResolvedTool<Compiler<C>> resolveCompiler(Class<C> specType, P requirement) {
            if (requirement instanceof JavaPlatform) {
              return new DefaultResolvedCompiler<C>(toolChain.select((JavaPlatform) requirement), specType);
            }
            return oldToolResolver.resolveCompiler(specType, requirement);
          }
        });
      }
    };

    final TaskCollection<JavaCompile> javaCompileTasks = project.getTasks().withType(JavaCompile.class);
    javaCompileTasks.all(action);
    javaCompileTasks.whenTaskAdded(action);
  }
}
