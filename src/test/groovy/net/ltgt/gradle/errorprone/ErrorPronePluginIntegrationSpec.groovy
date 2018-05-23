package net.ltgt.gradle.errorprone

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.junit.Assume.assumeTrue

class ErrorPronePluginIntegrationSpec extends Specification {
  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile

  final String testGradleVersion = System.getProperty("test.gradle-version", GradleVersion.current().version)

  def setup() {
    buildFile = testProjectDir.newFile('build.gradle')
    buildFile << """\
      buildscript {
        dependencies {
          classpath files(\$/${System.getProperty('plugin')}/\$)
        }
      }
      apply plugin: 'net.ltgt.errorprone'
      apply plugin: 'java'

      repositories {
        mavenCentral()
      }
      dependencies {
        errorprone fileTree(\$/${System.getProperty('dependencies')}/\$)
      }
    """.stripIndent()
  }

  def "compilation succeeds"() {
    given:
    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Success.java')
    f.createNewFile()
    getClass().getResource("/test/Success.java").withInputStream { f << it }

    when:
    def result = GradleRunner.create()
        .withGradleVersion(testGradleVersion)
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileJava')
        .build()

    then:
    result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS
  }

  def "compilation fails"() {
    given:
    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Failure.java')
    f.createNewFile()
    getClass().getResource("/test/Failure.java").withInputStream { f << it }

    when:
    def result = GradleRunner.create()
        .withGradleVersion(testGradleVersion)
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileJava')
        .buildAndFail()

    then:
    result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.FAILED
    result.output.contains("Failure.java:6: error: [ArrayEquals]")
  }

  def "compatible with JDK 9 --release flag"() {
    assumeTrue(JavaVersion.current().isJava9Compatible())
    assumeTrue(GradleVersion.version(testGradleVersion) >= GradleVersion.version("4.3"))

    given:
    buildFile << """\
      compileJava.options.compilerArgs << '--release' << '8'
    """.stripIndent()

    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Success.java')
    f.createNewFile()
    getClass().getResource("/test/Success.java").withInputStream { f << it }

    when:
    def result = GradleRunner.create()
            .withGradleVersion(testGradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments('--info', 'compileJava')
            .build()

    then:
    result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS
  }

  def "can configure errorprone"() {
    assumeTrue(GradleVersion.version(testGradleVersion) >= GradleVersion.version("4.6"))

    given:
    buildFile << """\
      import net.ltgt.gradle.errorprone.CheckSeverity

      compileJava.options.errorprone {
        check("ArrayEquals", CheckSeverity.OFF)
      }
    """.stripIndent()

    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Failure.java')
    f.createNewFile()
    getClass().getResource("/test/Failure.java").withInputStream { f << it }

    when:
    def result = GradleRunner.create()
            .withGradleVersion(testGradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments('--info', 'compileJava')
            .build()

    then:
    result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS
  }
}
