package net.ltgt.gradle.errorprone;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ErrorProneBasePlugin implements Plugin<Project> {

  public static final String CONFIGURATION_NAME = "errorprone";

  private static final String DEFAULT_DEPENDENCY =
      "com.google.errorprone:error_prone_core:latest.release";

  @Override
  public void apply(final Project project) {
    project
        .getConfigurations()
        .create(
            CONFIGURATION_NAME,
            files -> {
              files.setVisible(false);
              files.defaultDependencies(
                  dependencies ->
                      dependencies.add(project.getDependencies().create(DEFAULT_DEPENDENCY)));
            });
  }
}
