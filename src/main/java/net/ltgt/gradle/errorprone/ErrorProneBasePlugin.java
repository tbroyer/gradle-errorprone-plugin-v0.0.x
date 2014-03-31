package net.ltgt.gradle.errorprone;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class ErrorProneBasePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getConfigurations().create("errorprone", new Action<Configuration>() {
      @Override
      public void execute(Configuration files) {
        files.setVisible(false);
      }
    });
    project.getDependencies().add("errorprone", "com.google.errorprone:error_prone_core:latest.release");
  }
}
