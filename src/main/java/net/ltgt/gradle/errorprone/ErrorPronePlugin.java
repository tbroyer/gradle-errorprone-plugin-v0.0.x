package net.ltgt.gradle.errorprone;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.util.GradleVersion;

public class ErrorPronePlugin implements Plugin<Project> {
  @Override
  public void apply(final Project project) {
    project.getPluginManager().apply(ErrorProneBasePlugin.class);

    final boolean supportsCommandLineArgumentProvider =
        GradleVersion.current().compareTo(GradleVersion.version("4.6")) >= 0;
    final ErrorProneToolChain toolChain = ErrorProneToolChain.create(project);
    project
        .getTasks()
        .withType(JavaCompile.class)
        .all(
            task -> {
              task.setToolChain(toolChain);

              if (supportsCommandLineArgumentProvider) {
                final ErrorProneOptions errorproneOptions =
                    ((ExtensionAware) task.getOptions())
                        .getExtensions()
                        .create(
                            ErrorProneOptions.NAME, ErrorProneOptions.class, project.getObjects());
                task.getOptions().getCompilerArgumentProviders().add(errorproneOptions);
              }
            });
  }
}
