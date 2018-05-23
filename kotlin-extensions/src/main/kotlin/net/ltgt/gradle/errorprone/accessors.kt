package net.ltgt.gradle.errorprone

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.kotlin.dsl.* // ktlint-disable no-wildcard-imports

fun ErrorProneOptions.check(vararg pairs: Pair<String, CheckSeverity>) = checks.putAll(pairs)
fun ErrorProneOptions.option(vararg pairs: Pair<String, String>) = checkOptions.putAll(pairs)

val CompileOptions.errorprone: ErrorProneOptions
    get() = (this as ExtensionAware).extensions.getByName<ErrorProneOptions>(ErrorProneOptions.NAME)

operator fun ErrorProneOptions.invoke(configure: ErrorProneOptions.() -> Unit): ErrorProneOptions =
    apply(configure)
