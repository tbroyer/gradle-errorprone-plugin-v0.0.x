package net.ltgt.gradle.errorprone;

import java.util.Collections;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.JavaCompile;

public class ErrorPronePlugin implements Plugin<Project> {
  @Override
  public void apply(final Project project) {
    project.apply(Collections.singletonMap("plugin", ErrorProneBasePlugin.class));

    final ErrorProneToolChain toolChain = ErrorProneToolChain.create(project);
    final Action<JavaCompile> action =
        new Action<JavaCompile>() {
          @Override
          public void execute(JavaCompile task) {
            task.setToolChain(toolChain);
          }
        };

    final TaskCollection<JavaCompile> javaCompileTasks =
        project.getTasks().withType(JavaCompile.class);
    javaCompileTasks.all(action);
    javaCompileTasks.whenTaskAdded(action);
  }
}
