package net.ltgt.gradle.errorprone;

import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.process.CommandLineArgumentProvider;

public class ErrorProneOptions {
  public static final String NAME = "errorprone";

  private final Property<Boolean> isEnabled;
  private final Property<Boolean> disableAllChecks;
  private final Property<Boolean> allErrorsAsWarnings;
  private final Property<Boolean> allDisabledChecksAsWarnings;
  private final Property<Boolean> disableWarningsInGeneratedCode;
  private final Property<Boolean> ignoreUnknownCheckNames;
  private final Property<Boolean> isCompilingTestOnlyCode;
  private final Property<String> excludedPaths;
  private final MapProperty<String, CheckSeverity> checks;
  private final MapProperty<String, String> checkOptions;
  private final ListProperty<String> errorproneArgs;
  private final ListProperty<CommandLineArgumentProvider> errorproneArgumentProviders;

  @SuppressWarnings("unchecked")
  @Inject
  public ErrorProneOptions(ObjectFactory objectFactory) {
    isEnabled = objectFactory.property(Boolean.class);
    isEnabled.set(true);
    disableAllChecks = objectFactory.property(Boolean.class);
    allErrorsAsWarnings = objectFactory.property(Boolean.class);
    allDisabledChecksAsWarnings = objectFactory.property(Boolean.class);
    disableWarningsInGeneratedCode = objectFactory.property(Boolean.class);
    ignoreUnknownCheckNames = objectFactory.property(Boolean.class);
    isCompilingTestOnlyCode = objectFactory.property(Boolean.class);
    excludedPaths = objectFactory.property(String.class);
    checks = DefaultMapProperty.create(objectFactory);
    checkOptions = DefaultMapProperty.create(objectFactory);
    errorproneArgs = objectFactory.listProperty(String.class);
    errorproneArgumentProviders = objectFactory.listProperty(CommandLineArgumentProvider.class);
  }

  @Input
  @Optional
  public Property<Boolean> getIsEnabled() {
    return isEnabled;
  }

  @Input
  @Optional
  public Property<Boolean> getDisableAllChecks() {
    return disableAllChecks;
  }

  @Input
  @Optional
  public Property<Boolean> getAllErrorsAsWarnings() {
    return allErrorsAsWarnings;
  }

  @Input
  @Optional
  public Property<Boolean> getAllDisabledChecksAsWarnings() {
    return allDisabledChecksAsWarnings;
  }

  @Input
  @Optional
  public Property<Boolean> getDisableWarningsInGeneratedCode() {
    return disableWarningsInGeneratedCode;
  }

  @Input
  @Optional
  public Property<Boolean> getIgnoreUnknownCheckNames() {
    return ignoreUnknownCheckNames;
  }

  @Input
  @Optional
  public Property<Boolean> getIsCompilingTestOnlyCode() {
    return isCompilingTestOnlyCode;
  }

  @Input
  @Optional
  public Property<String> getExcludedPaths() {
    return excludedPaths;
  }

  @Input
  public MapProperty<String, CheckSeverity> getChecks() {
    return checks;
  }

  public void setChecks(Map<String, CheckSeverity> checks) {
    this.checks.set(checks);
  }

  public void check(String... checkNames) {
    for (String checkName : checkNames) {
      checks.put(checkName, CheckSeverity.DEFAULT);
    }
  }

  public void check(String checkName, CheckSeverity severity) {
    checks.put(checkName, severity);
  }

  @Input
  public MapProperty<String, String> getCheckOptions() {
    return checkOptions;
  }

  public void setCheckOptions(Map<String, String> checkOptions) {
    this.checkOptions.set(checkOptions);
  }

  public void option(String name) {
    checkOptions.put(name, "true");
  }

  public void option(String name, String value) {
    checkOptions.put(name, value);
  }

  @Input
  public ListProperty<String> getErrorproneArgs() {
    return errorproneArgs;
  }

  @Nested
  public ListProperty<CommandLineArgumentProvider> getErrorproneArgumentProviders() {
    return errorproneArgumentProviders;
  }

  void applyDefaults(ErrorProneOptions defaultOptions) {
    isEnabled.set(defaultOptions.isEnabled);
    disableAllChecks.set(defaultOptions.disableAllChecks);
    allErrorsAsWarnings.set(defaultOptions.allErrorsAsWarnings);
    allDisabledChecksAsWarnings.set(defaultOptions.allDisabledChecksAsWarnings);
    disableWarningsInGeneratedCode.set(defaultOptions.disableWarningsInGeneratedCode);
    ignoreUnknownCheckNames.set(defaultOptions.ignoreUnknownCheckNames);
    isCompilingTestOnlyCode.set(defaultOptions.isCompilingTestOnlyCode);
    excludedPaths.set(defaultOptions.excludedPaths);
    checks.set(defaultOptions.checks);
    checkOptions.set(defaultOptions.checkOptions);
    errorproneArgs.set(defaultOptions.errorproneArgs);
    errorproneArgumentProviders.set(defaultOptions.errorproneArgumentProviders);
  }
}
