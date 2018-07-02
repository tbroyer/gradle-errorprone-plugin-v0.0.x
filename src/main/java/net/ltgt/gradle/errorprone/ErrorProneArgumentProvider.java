package net.ltgt.gradle.errorprone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import org.gradle.api.InvalidUserDataException;
import org.gradle.process.CommandLineArgumentProvider;

public class ErrorProneArgumentProvider implements CommandLineArgumentProvider {
  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\p{IsWhite_Space}");

  private static String validate(String arg) {
    if (WHITESPACE_PATTERN.matcher(arg).find()) {
      throw new InvalidUserDataException(
          "Error Prone options cannot contain white space: \"" + arg + "\".");
    }
    return arg;
  }

  private static <T extends Iterable<String>> T validate(T args) {
    args.forEach(ErrorProneArgumentProvider::validate);
    return args;
  }

  private static void validateName(Map.Entry<String, ?> arg) {
    if (arg.getKey().contains(":")) {
      throw new InvalidUserDataException(
          "Error Prone check name cannot contain a colon (\":\"): \"" + arg.getKey() + "\".");
    }
  }

  private final ErrorProneOptions options;

  public ErrorProneArgumentProvider(ErrorProneOptions options) {
    this.options = options;
  }

  @Override
  public Iterable<String> asArguments() {
    if (!options.getIsEnabled().getOrElse(Boolean.TRUE)) {
      return Collections.emptyList();
    }
    ArrayList<String> args = new ArrayList<>();
    if (options.getDisableAllChecks().getOrElse(Boolean.FALSE)) {
      args.add("-XepDisableAllChecks");
    }
    if (options.getAllErrorsAsWarnings().getOrElse(Boolean.FALSE)) {
      args.add("-XepAllErrorsAsWarnings");
    }
    if (options.getAllDisabledChecksAsWarnings().getOrElse(Boolean.FALSE)) {
      args.add("-XepAllDisabledChecksAsWarnings");
    }
    if (options.getDisableWarningsInGeneratedCode().getOrElse(Boolean.FALSE)) {
      args.add("-XepDisableWarningsInGeneratedCode");
    }
    if (options.getIgnoreUnknownCheckNames().getOrElse(Boolean.FALSE)) {
      args.add("-XepIgnoreUnknownCheckNames");
    }
    if (options.getIsCompilingTestOnlyCode().getOrElse(Boolean.FALSE)) {
      args.add("-XepCompilingTestOnlyCode");
    }
    if (options.getExcludedPaths().isPresent() && !options.getExcludedPaths().get().isEmpty()) {
      args.add(validate("-XepExcludedPaths:" + options.getExcludedPaths().get()));
    }
    options
        .getChecks()
        .getOrElse(Collections.emptyMap())
        .entrySet()
        .stream()
        .peek(ErrorProneArgumentProvider::validateName)
        .map(
            e ->
                "-Xep:"
                    + e.getKey()
                    + ((e.getValue() == CheckSeverity.DEFAULT) ? "" : ":" + e.getValue().name()))
        .peek(ErrorProneArgumentProvider::validate)
        .forEach(args::add);
    options
        .getCheckOptions()
        .getOrElse(Collections.emptyMap())
        .entrySet()
        .stream()
        .map(e -> "-XepOpt:" + e.getKey() + "=" + e.getValue())
        .peek(ErrorProneArgumentProvider::validate)
        .forEach(args::add);
    args.addAll(validate(options.getErrorproneArgs().get()));
    options
        .getErrorproneArgumentProviders()
        .getOrElse(Collections.emptyList())
        .stream()
        .map(CommandLineArgumentProvider::asArguments)
        .forEach(it -> it.forEach(arg -> args.add(validate(arg))));
    return args;
  }
}
