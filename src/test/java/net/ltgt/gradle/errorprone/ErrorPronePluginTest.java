package net.ltgt.gradle.errorprone;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collections;

import org.gradle.api.Project;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.internal.tasks.compile.CompilationFailedException;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

public class ErrorPronePluginTest {
  @Test
  public void shouldSucceed() {
    Project project = ProjectBuilder.builder()
        .withProjectDir(new File("integrationTests/success"))
        .build();
    project.apply(Collections.singletonMap("plugin", "java"));
    project.apply(Collections.singletonMap("plugin", ErrorPronePlugin.class));
    project.getRepositories().mavenCentral();

    final AbstractTask compileJavaTask = (AbstractTask) project.getTasks().getByName("compileJava");
    compileJavaTask.execute();
    assertTrue(compileJavaTask.getDidWork());
  }

  @Test
  public void shouldFail() {
    Project project = ProjectBuilder.builder()
        .withProjectDir(new File("integrationTests/failure"))
        .build();
    project.apply(Collections.singletonMap("plugin", "java"));
    project.apply(Collections.singletonMap("plugin", ErrorPronePlugin.class));
    project.getRepositories().mavenCentral();

    try {
      ((AbstractTask) project.getTasks().getByName("compileJava")).execute();
      fail();
    } catch (TaskExecutionException tee) {
      assertTrue(tee.getCause() instanceof CompilationFailedException);
    }
  }
}
