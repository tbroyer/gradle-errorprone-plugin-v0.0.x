package net.ltgt.gradle.errorprone;

import java.util.HashMap;
import java.util.Map;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.util.GradleVersion;

public class ErrorProneBasePlugin implements Plugin<Project> {
  private static final Logger logger = Logging.getLogger(ErrorProneBasePlugin.class);

  public static final String CONFIGURATION_NAME = "errorprone";

  static final String WARNING_MESSAGE =
      "This build is using the default Error Prone dependencies, "
          + "which always uses the latest release of Error Prone, "
          + "and might break your build at any time. "
          + "Configure the Error Prone dependencies explicitly to silence this warning.";

  static final boolean SUPPORTS_COMMAND_LINE_ARGUMENT_PROVIDER =
      GradleVersion.current().compareTo(GradleVersion.version("4.6")) >= 0;

  @Override
  public void apply(final Project project) {
    if (SUPPORTS_COMMAND_LINE_ARGUMENT_PROVIDER) {
      final ErrorProneExtension errorproneExtension =
          project
              .getExtensions()
              .create(ErrorProneExtension.NAME, ErrorProneExtension.class, project.getObjects());
      project
          .getConfigurations()
          .create(
              CONFIGURATION_NAME,
              files -> {
                files.setVisible(false);
                files.defaultDependencies(
                    dependencies -> {
                      String version = errorproneExtension.getToolVersion().getOrNull();
                      if (version == null) {
                        logger.warn(WARNING_MESSAGE);
                        version = "latest.release";
                      }
                      Map<String, String> dependency = new HashMap<>();
                      dependency.put("group", "com.google.errorprone");
                      dependency.put("name", "error_prone_core");
                      dependency.put("version", version);
                      dependencies.add(project.getDependencies().create(dependency));
                    });
              });
    } else {
      project
          .getConfigurations()
          .create(
              CONFIGURATION_NAME,
              files -> {
                files.setVisible(false);
                files.defaultDependencies(
                    dependencies -> {
                      logger.warn(WARNING_MESSAGE);
                      dependencies.add(
                          project
                              .getDependencies()
                              .create("com.google.errorprone:error_prone_core:latest.release"));
                    });
              });
    }
  }
}
