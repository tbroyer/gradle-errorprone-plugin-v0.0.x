import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-gradle-plugin`
    groovy
    id("com.gradle.plugin-publish") version "0.9.10"
    id("com.github.sherter.google-java-format") version "0.6"
    id("net.ltgt.errorprone") version "0.0.13"
}

googleJavaFormat {
    toolVersion = "1.5"
}

group = "net.ltgt.gradle"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
if (JavaVersion.current().isJava9Compatible) {
    tasks.withType<JavaCompile> { options.compilerArgs.addAll(arrayOf("--release", "8")) }
    tasks.withType<GroovyCompile> { options.compilerArgs.addAll(arrayOf("--release", "8")) }
}

gradle.taskGraph.whenReady {
    if (hasTask(tasks["publishPlugins"])) {
        check("git diff --quiet --exit-code".execute(null, rootDir).waitFor() == 0, { "Working tree is dirty" })
        val process = "git describe --exact-match".execute(null, rootDir)
        check(process.waitFor() == 0, { "Version is not tagged" })
        version = process.text.trim().removePrefix("v")
    }
}

repositories {
    jcenter()
}
dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.3.1")

    testImplementation(localGroovy())
    testImplementation("com.netflix.nebula:nebula-test:6.4.2")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(group = "org.codehaus.groovy")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(arrayOf("-Xlint:all", "-Werror"))
}

val integTest by configurations.creating

dependencies {
    integTest("com.google.errorprone:error_prone_core:2.3.1")
}

val prepareIntegTestDependencies by tasks.creating(Copy::class) {
    from(integTest)
    into("$buildDir/integTestDependencies/")
}

val test by tasks.getting(Test::class) {
    val jar: Jar by tasks.getting

    val testGradleVersion = project.findProperty("test.gradle-version")
    testGradleVersion?.also { systemProperty("test.gradle-version", testGradleVersion) }

    inputs.files(prepareIntegTestDependencies).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(jar).withPathSensitivity(PathSensitivity.NONE)

    systemProperty("dependencies", prepareIntegTestDependencies.destinationDir)
    systemProperty("plugin", jar.archivePath)

    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

gradlePlugin {
    (plugins) {
        "errorpronePlugin" {
            id = "net.ltgt.errorprone"
            implementationClass = "net.ltgt.gradle.errorprone.ErrorPronePlugin"
        }
        "errorproneBasePlugin" {
            id = "net.ltgt.errorprone-base"
            implementationClass = "net.ltgt.gradle.errorprone.ErrorProneBasePlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/tbroyer/gradle-errorprone-plugin"
    vcsUrl = "https://github.com/tbroyer/gradle-errorprone-plugin"
    description = "Gradle plugin to use the error-prone compiler for Java"
    tags = listOf("javac", "error-prone")

    (plugins) {
        "errorpronePlugin" {
            id = "net.ltgt.errorprone"
            displayName = "Gradle error-prone plugin"
        }
        "errorproneBasePlugin" {
            id = "net.ltgt.errorprone-base"
            displayName = "Gradle error-prone base plugin"
        }
    }

    mavenCoordinates {
        groupId = project.group.toString()
        artifactId = project.name
    }
}

val ktlint by configurations.creating

dependencies {
    ktlint("com.github.shyiko:ktlint:0.22.0")
}

val verifyKtlint by tasks.creating(JavaExec::class) {
    description = "Check Kotlin code style."
    classpath = ktlint
    main = "com.github.shyiko.ktlint.Main"
    args("**./*.gradle.kts", "**/*.kt")
}
tasks["check"].dependsOn(verifyKtlint)

task("ktlint", JavaExec::class) {
    description = "Fix Kotlin code style violations."
    classpath = verifyKtlint.classpath
    main = verifyKtlint.main
    args("-F")
    args(verifyKtlint.args)
}

fun String.execute(envp: Array<String>?, workingDir: File?) =
    Runtime.getRuntime().exec(this, envp, workingDir)

val Process.text: String
    get() = inputStream.bufferedReader().readText()
