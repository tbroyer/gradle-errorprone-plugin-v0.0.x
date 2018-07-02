package net.ltgt.gradle.errorprone

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.kotlin.dsl.* // ktlint-disable no-wildcard-imports

fun ErrorProneOptions.check(vararg pairs: Pair<String, CheckSeverity>) = pairs.forEach { (k, v) -> check(k, v) }
fun ErrorProneOptions.option(vararg pairs: Pair<String, String>) = pairs.forEach { (k, v) -> option(k, v) }

val CompileOptions.errorprone: ErrorProneOptions
    get() = (this as ExtensionAware).extensions.getByName<ErrorProneOptions>(ErrorProneOptions.NAME)

fun CompileOptions.errorprone(configure: ErrorProneOptions.() -> Unit) =
    (this as ExtensionAware).extensions.configure(ErrorProneOptions.NAME, configure)
