package net.ltgt.gradle.errorprone;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class ErrorProneBasePlugin implements Plugin<Project> {
  private static final Logger logger = Logging.getLogger(ErrorProneBasePlugin.class);

  public static final String CONFIGURATION_NAME = "errorprone";

  private static final String DEFAULT_DEPENDENCY =
      "com.google.errorprone:error_prone_core:latest.release";

  static final String WARNING_MESSAGE =
      "This build is using the default Error Prone dependencies, "
          + "which always uses the latest release of Error Prone, "
          + "and might break your build at any time. "
          + "Configure the Error Prone dependencies explicitly to silence this warning.";

  @Override
  public void apply(final Project project) {
    project
        .getConfigurations()
        .create(
            CONFIGURATION_NAME,
            files -> {
              files.setVisible(false);
              files.defaultDependencies(
                  dependencies -> {
                    logger.warn(WARNING_MESSAGE);
                    dependencies.add(project.getDependencies().create(DEFAULT_DEPENDENCY));
                  });
            });
  }
}
