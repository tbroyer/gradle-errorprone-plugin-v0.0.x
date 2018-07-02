package net.ltgt.gradle.errorprone;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.compile.JavaCompile;

public class ErrorPronePlugin implements Plugin<Project> {
  @Override
  public void apply(final Project project) {
    project.getPluginManager().apply(ErrorProneBasePlugin.class);

    final ErrorProneToolChain toolChain = ErrorProneToolChain.create(project);
    project.getTasks().withType(JavaCompile.class).all(task -> task.setToolChain(toolChain));

    if (ErrorProneBasePlugin.SUPPORTS_COMMAND_LINE_ARGUMENT_PROVIDER) {
      final ErrorProneExtension errorproneExtension =
          project.getExtensions().getByType(ErrorProneExtension.class);
      project
          .getTasks()
          .withType(JavaCompile.class)
          .all(
              task -> {
                final ErrorProneOptions errorproneOptions =
                    ((ExtensionAware) task.getOptions())
                        .getExtensions()
                        .create(
                            ErrorProneOptions.NAME, ErrorProneOptions.class, project.getObjects());
                errorproneOptions.applyDefaults(errorproneExtension.getDefaultOptions());
                task.getOptions()
                    .getCompilerArgumentProviders()
                    .add(new ErrorProneArgumentProvider(errorproneOptions));
              });
    }
  }
}
