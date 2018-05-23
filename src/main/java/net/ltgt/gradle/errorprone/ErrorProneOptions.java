package net.ltgt.gradle.errorprone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.process.CommandLineArgumentProvider;

public class ErrorProneOptions implements CommandLineArgumentProvider {
  public static final String NAME = "errorprone";

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\p{IsWhite_Space}");

  private static String validate(String arg) {
    if (WHITESPACE_PATTERN.matcher(arg).find()) {
      throw new InvalidUserDataException(
          "Error Prone options cannot contain white space: \"" + arg + "\".");
    }
    return arg;
  }

  private static <T extends Iterable<String>> T validate(T args) {
    args.forEach(ErrorProneOptions::validate);
    return args;
  }

  private static void validateName(Map.Entry<String, ?> arg) {
    if (arg.getKey().contains(":")) {
      throw new InvalidUserDataException(
          "Error Prone check name cannot contain a colon (\":\"): \"" + arg.getKey() + "\".");
    }
  }

  private final Property<Boolean> isEnabled;
  private final Property<Boolean> disableAllChecks;
  private final Property<Boolean> allErrorsAsWarnings;
  private final Property<Boolean> allDisabledChecksAsWarnings;
  private final Property<Boolean> disableWarningsInGeneratedCode;
  private final Property<Boolean> ignoreUnknownCheckNames;
  private final Property<Boolean> isCompilingTestOnlyCode;
  private final Property<String> excludedPaths;
  private Map<String, CheckSeverity> checks = new LinkedHashMap<>();
  private Map<String, String> checkOptions = new LinkedHashMap<>();
  private final ListProperty<String> errorproneArgs;
  private final List<CommandLineArgumentProvider> errorproneArgumentProviders = new ArrayList<>();

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
    errorproneArgs = objectFactory.listProperty(String.class);
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
  public Map<String, CheckSeverity> getChecks() {
    return checks;
  }

  public void setChecks(Map<String, CheckSeverity> checks) {
    this.checks = checks;
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
  public Map<String, String> getCheckOptions() {
    return checkOptions;
  }

  public void setCheckOptions(Map<String, String> checkOptions) {
    this.checkOptions = checkOptions;
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
  public List<CommandLineArgumentProvider> getErrorproneArgumentProviders() {
    return errorproneArgumentProviders;
  }

  @Override
  public Iterable<String> asArguments() {
    if (!isEnabled.getOrElse(Boolean.TRUE)) {
      return Collections.emptyList();
    }
    ArrayList<String> args = new ArrayList<>();
    if (disableAllChecks.getOrElse(Boolean.FALSE)) {
      args.add("-XepDisableAllChecks");
    }
    if (allErrorsAsWarnings.getOrElse(Boolean.FALSE)) {
      args.add("-XepAllErrorsAsWarnings");
    }
    if (allDisabledChecksAsWarnings.getOrElse(Boolean.FALSE)) {
      args.add("-XepAllDisabledChecksAsWarnings");
    }
    if (disableWarningsInGeneratedCode.getOrElse(Boolean.FALSE)) {
      args.add("-XepDisableWarningsInGeneratedCode");
    }
    if (ignoreUnknownCheckNames.getOrElse(Boolean.FALSE)) {
      args.add("-XepIgnoreUnknownCheckNames");
    }
    if (isCompilingTestOnlyCode.getOrElse(Boolean.FALSE)) {
      args.add("-XepCompilingTestOnlyCode");
    }
    if (excludedPaths.isPresent() && !excludedPaths.get().isEmpty()) {
      args.add(validate("-XepExcludedPaths:" + excludedPaths.get()));
    }
    checks
        .entrySet()
        .stream()
        .peek(ErrorProneOptions::validateName)
        .map(
            e ->
                "-Xep:"
                    + e.getKey()
                    + ((e.getValue() == CheckSeverity.DEFAULT) ? "" : ":" + e.getValue().name()))
        .peek(ErrorProneOptions::validate)
        .forEach(args::add);
    checkOptions
        .entrySet()
        .stream()
        .map(e -> "-XepOpt:" + e.getKey() + "=" + e.getValue())
        .peek(ErrorProneOptions::validate)
        .forEach(args::add);
    args.addAll(validate(errorproneArgs.get()));
    errorproneArgumentProviders
        .stream()
        .map(CommandLineArgumentProvider::asArguments)
        .forEach(it -> it.forEach(arg -> args.add(validate(arg))));
    return args;
  }
}
