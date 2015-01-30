package net.ltgt.gradle.errorprone;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class ErrorProneBasePlugin implements Plugin<Project> {

  public static final String CONFIGURATION_NAME = "errorprone";

  @Override
  public void apply(Project project) {
    project.getConfigurations().create(CONFIGURATION_NAME, new Action<Configuration>() {
      @Override
      public void execute(Configuration files) {
        files.setVisible(false);
      }
    });
    project.getDependencies().add(CONFIGURATION_NAME, "com.google.errorprone:error_prone_core:latest.release");
  }
}
