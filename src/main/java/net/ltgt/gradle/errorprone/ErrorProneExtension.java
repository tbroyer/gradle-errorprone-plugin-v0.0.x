package net.ltgt.gradle.errorprone;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;

public class ErrorProneExtension {
  public static final String NAME = "errorprone";

  private final ErrorProneOptions defaultOptions;

  public ErrorProneExtension(ObjectFactory objectFactory) {
    defaultOptions = new ErrorProneOptions(objectFactory);
  }

  public ErrorProneOptions getDefaultOptions() {
    return defaultOptions;
  }

  public void defaultOptions(Action<ErrorProneOptions> configure) {
    configure.execute(defaultOptions);
  }
}
