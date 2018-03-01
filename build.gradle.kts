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
    if (hasTask("publishPlugins")) {
        assert("git diff --quiet --exit-code".execute(null, rootDir).waitFor() == 0, { "Working tree is dirty" })
        val process = "git describe --exact-match".execute(null, rootDir)
        assert(process.waitFor() == 0, { "Version is not tagged" })
        version = process.text.trim().removePrefix("v")
    }
}

repositories {
    jcenter()
}
dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.2.0")

    testImplementation(localGroovy())
    testImplementation("com.netflix.nebula:nebula-test:6.1.2")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(group = "org.codehaus.groovy")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(arrayOf("-Xlint:all", "-Werror"))
}

val integTest by configurations.creating

dependencies {
    integTest("com.google.errorprone:error_prone_core:2.2.0")
}

val prepareIntegTestDependencies by tasks.creating(Copy::class) {
    from(integTest)
    into("$buildDir/integTestDependencies/")
}

val test by tasks.getting(Test::class) {
    val testGradleVersions = project.findProperty("test.gradle-versions") as? String
    val jar: Jar by tasks.getting

    inputs.files(prepareIntegTestDependencies).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(jar).withPathSensitivity(PathSensitivity.NONE)
    inputs.property("test.gradle-versions", testGradleVersions).optional(true)

    systemProperty("dependencies", prepareIntegTestDependencies.destinationDir)
    systemProperty("plugin", jar.archivePath)
    if (!testGradleVersions.isNullOrBlank()) {
        systemProperty("test.gradle-versions", testGradleVersions!!)
    }

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

fun String.execute(envp: Array<String>?, workingDir: File?) =
    Runtime.getRuntime().exec(this, envp, workingDir)

val Process.text: String
    get() = inputStream.bufferedReader().readText()
