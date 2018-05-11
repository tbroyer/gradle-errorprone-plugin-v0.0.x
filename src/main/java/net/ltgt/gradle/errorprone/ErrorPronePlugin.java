package net.ltgt.gradle.errorprone;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

public class ErrorPronePlugin implements Plugin<Project> {
  @Override
  public void apply(final Project project) {
    project.getPluginManager().apply(ErrorProneBasePlugin.class);

    final ErrorProneToolChain toolChain = ErrorProneToolChain.create(project);
    project.getTasks().withType(JavaCompile.class).all(task -> task.setToolChain(toolChain));
  }
}
