package net.ltgt.gradle.errorprone;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;

public class ErrorProneBasePlugin implements Plugin<Project> {

  public static final String CONFIGURATION_NAME = "errorprone";

  private static final String DEFAULT_DEPENDENCY = "com.google.errorprone:error_prone_core:latest.release";

  @Override
  public void apply(final Project project) {
    project.getConfigurations().create(CONFIGURATION_NAME, new Action<Configuration>() {
      @Override
      public void execute(Configuration files) {
        files.setVisible(false);
        files.defaultDependencies(new Action<DependencySet>() {
          @Override
          public void execute(DependencySet dependencies) {
            dependencies.add(project.getDependencies().create(DEFAULT_DEPENDENCY));
          }
        });
      }
    });
  }
}
