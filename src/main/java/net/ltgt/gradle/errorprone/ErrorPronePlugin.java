package net.ltgt.gradle.errorprone;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskInputs;
import org.gradle.api.tasks.compile.JavaCompile;

public class ErrorPronePlugin implements Plugin<Project> {
  @Override
  public void apply(final Project project) {
    project.getPluginManager().apply(ErrorProneBasePlugin.class);

    final ErrorProneToolChain toolChain = ErrorProneToolChain.create(project);
    project
        .getTasks()
        .withType(JavaCompile.class)
        .all(
            task -> {
              task.setToolChain(toolChain);
              taskGetInputsFiles(task, toolChain.getConfiguration());
            });
  }

  private void taskGetInputsFiles(JavaCompile task, Configuration configuration) {
    try {
      // task.getInputs().files(configuration)…
      Object inputs =
          taskInputsFiles.invoke(taskGetInputs.invoke(task), (Object) new Object[] {configuration});
      if (withNormalizer != null) {
        // ….withNormalizer(ClasspathNormalizer.class)
        withNormalizer.invoke(inputs, classpathNormalizer);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private static final Method taskGetInputs;
  private static final Method taskInputsFiles;
  private static final @Nullable Method withNormalizer;
  private static final @Nullable Class<?> classpathNormalizer;

  static {
    // Changed return type in Gradle 3.0
    taskGetInputs = getMethod(Task.class, "getInputs");
    taskInputsFiles = getMethod(TaskInputs.class, "files", Object[].class);

    // Only exists since Gradle 4.3
    classpathNormalizer = classForName("org.gradle.api.tasks.ClasspathNormalizer");
    if (classpathNormalizer != null) {
      // Exists since Gradle 3.1
      Class<?> taskInputFilePropertyBuilder =
          requireNonNull(classForName("org.gradle.api.tasks.TaskInputFilePropertyBuilder"));
      withNormalizer = getMethod(taskInputFilePropertyBuilder, "withNormalizer", Class.class);
    } else {
      withNormalizer = null;
    }
  }

  private static @Nullable Class<?> classForName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
    try {
      return clazz.getMethod(methodName, parameterTypes);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}
