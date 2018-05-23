package net.ltgt.gradle.errorprone

import com.google.errorprone.InvalidCommandLineOptionException
import org.gradle.process.CommandLineArgumentProvider

import nebula.test.ProjectSpec
import org.gradle.api.InvalidUserDataException
import com.google.errorprone.ErrorProneOptions.Severity

class ErrorProneOptionsSpec extends ProjectSpec {
    def 'generates correct error prone options'() {
        when:
        def options = new ErrorProneOptions(project.objects)
        options.with(configure)
        def parsedOptions = parseOptions(options)

        then:
        assertOptionsEqual(options, parsedOptions)

        where:
        configure << [
            { disableAllChecks.set(true) },
            { allErrorsAsWarnings.set(true) },
            { allDisabledChecksAsWarnings.set(true) },
            { disableWarningsInGeneratedCode.set(true) },
            { ignoreUnknownCheckNames.set(true) },
            { isCompilingTestOnlyCode.set(true) },
            { excludedPaths.set(".*/build/generated/.*") },
            { check("ArrayEquals") },
            { check("ArrayEquals", CheckSeverity.WARN) },
            { checks["ArrayEquals"] = CheckSeverity.ERROR },
            { checks = ["ArrayEquals": CheckSeverity.DEFAULT] },
            { option("Foo") },
            { option("Foo", "Bar") },
            { checkOptions["Foo"] = "Bar" },
            { checkOptions = ["Foo": "Bar"] },
            {
                disableAllChecks.set(true)
                allErrorsAsWarnings.set(true)
                allDisabledChecksAsWarnings.set(true)
                disableWarningsInGeneratedCode.set(true)
                ignoreUnknownCheckNames.set(true)
                isCompilingTestOnlyCode.set(true)
                excludedPaths.set(".*/build/generated/.*")
                check("BetaApi")
                check("NullAway", CheckSeverity.ERROR)
                option("Foo")
                option("NullAway:AnnotatedPackages", "net.ltgt.gradle.errorprone")
            },
        ]
    }

    def 'correctly passes free arguments'() {
        when:
        def referenceOptions = new ErrorProneOptions(project.objects)
        referenceOptions.with(reference)

        def options = new ErrorProneOptions(project.objects)
        options.with(configure)
        def parsedOptions = parseOptions(options)

        then:
        assertOptionsEqual(referenceOptions, parsedOptions)

        where:
        // We cannot test arguments that are not yet covered, and couldn't check patching options
        // (due to class visibility issue), so we're testing equivalence between free-form args
        // vs. args generated by flags (that we already test above on their own)
        [configure, reference] << [
            [ { errorproneArgs.add("-XepDisableAllChecks") }, { disableAllChecks.set(true) } ],

            [
                { errorproneArgs.set([ "-XepDisableAllChecks", "-Xep:BetaApi" ]) },
                { disableAllChecks.set(true); check("BetaApi") },
            ],

            [
                {
                    errorproneArgumentProviders.add({
                        [
                            "-Xep:NullAway:ERROR",
                            "-XepOpt:NullAway:AnnotatedPackages=net.ltgt.gradle.errorprone",
                        ]
                    } as CommandLineArgumentProvider)
                },
                {
                    check("NullAway", CheckSeverity.ERROR)
                    option("NullAway:AnnotatedPackages", "net.ltgt.gradle.errorprone")
                },
            ],
        ]
    }

    def 'rejects spaces'() {
        def options = new ErrorProneOptions(project.objects)
        options.with(configure)

        when:
        options.asArguments()

        then:
        InvalidUserDataException e = thrown()
        e.message.startsWith("""Error Prone options cannot contain white space: "$argPrefix""")

        where:
        [argPrefix, configure] << [
            [ "-XepExcludedPaths:", {
                excludedPaths.set("/home/user/My Projects/project-name/build/generated sources/.*")
            }],
            [ "-Xep:", { check("Foo Bar") } ],
            [ "-XepOpt:", { option("Foo Bar") } ],
            [ "-XepOpt:", { option("Foo", "Bar Baz") } ],
            [ "-Xep:Foo -Xep:Bar", { errorproneArgs.add("-Xep:Foo -Xep:Bar") } ],
            [ "-Xep:Foo -Xep:Bar", {
                errorproneArgumentProviders.add({ [ "-Xep:Foo -Xep:Bar" ] } as CommandLineArgumentProvider)
            }],
        ]
    }

    def 'rejects colon in check name'() {
        when:
        def options = new ErrorProneOptions(project.objects)
        options.check("ArrayEquals:OFF")
        options.asArguments()

        then:
        InvalidUserDataException e = thrown()
        e.message == """Error Prone check name cannot contain a colon (":"): "ArrayEquals:OFF"."""

        // Won't analyze free-form arguments, but those should be caught (later) by argument parsing
        // This test asserts that we're not being too restrictive, and only try to fail early.
        when:
        options = new ErrorProneOptions(project.objects)
        options.ignoreUnknownCheckNames.set(true)
        options.errorproneArgs.add("-Xep:Foo:Bar")
        parseOptions(options)

        then:
        thrown(InvalidCommandLineOptionException)
    }

    private com.google.errorprone.ErrorProneOptions parseOptions(ErrorProneOptions options) {
        return com.google.errorprone.ErrorProneOptions.processArgs(options.asArguments())
    }

    private void assertOptionsEqual(
        ErrorProneOptions options,
        com.google.errorprone.ErrorProneOptions parsedOptions
    ) {
        assert parsedOptions.disableAllChecks == options.disableAllChecks.getOrElse(false)
        assert parsedOptions.dropErrorsToWarnings == options.allErrorsAsWarnings.getOrElse(false)
        assert parsedOptions.enableAllChecksAsWarnings == options.allDisabledChecksAsWarnings.getOrElse(false)
        assert parsedOptions.disableWarningsInGeneratedCode() == options.disableWarningsInGeneratedCode.getOrElse(false)
        assert parsedOptions.ignoreUnknownChecks() == options.ignoreUnknownCheckNames.getOrElse(false)
        assert parsedOptions.testOnlyTarget == options.isCompilingTestOnlyCode.getOrElse(false)
        assert parsedOptions.excludedPattern?.pattern() == options.excludedPaths.getOrNull()
        assert parsedOptions.severityMap == options.checks.collectEntries { k, v -> [ k, toSeverity(v) ] }
        assert parsedOptions.flags.flagsMap == options.checkOptions
        assert parsedOptions.remainingArgs.length == 0
    }

    private Severity toSeverity(CheckSeverity checkSeverity) {
        switch (checkSeverity) {
            case CheckSeverity.DEFAULT: return Severity.DEFAULT
            case CheckSeverity.OFF: return Severity.OFF
            case CheckSeverity.WARN: return Severity.WARN
            case CheckSeverity.ERROR: return Severity.ERROR
            default: throw new AssertionError()
        }
    }
}