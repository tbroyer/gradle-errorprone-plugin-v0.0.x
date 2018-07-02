package net.ltgt.gradle.errorprone;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class ErrorProneExtension {
  public static final String NAME = "errorprone";

  private final Property<String> toolVersion;
  private final ErrorProneOptions defaultOptions;

  public ErrorProneExtension(ObjectFactory objectFactory) {
    toolVersion = objectFactory.property(String.class);
    defaultOptions = new ErrorProneOptions(objectFactory);
  }

  public Property<String> getToolVersion() {
    return toolVersion;
  }

  public ErrorProneOptions getDefaultOptions() {
    return defaultOptions;
  }

  public void defaultOptions(Action<ErrorProneOptions> configure) {
    configure.execute(defaultOptions);
  }
}
