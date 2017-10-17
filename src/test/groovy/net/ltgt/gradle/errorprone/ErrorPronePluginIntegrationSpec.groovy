package net.ltgt.gradle.errorprone

import org.gradle.internal.jvm.Jvm
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.Assume.assumeFalse

class ErrorPronePluginIntegrationSpec extends Specification {
  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile

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

  @Unroll
  def "compilation succeeds with Gradle #gradleVersion"() {
    given:
    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Success.java')
    f.createNewFile()
    getClass().getResource("/test/Success.java").withInputStream { f << it }

    when:
    def result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileJava')
        .build()

    then:
    result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << IntegrationTestHelper.GRADLE_VERSIONS
  }

  @Unroll
  def "compilation fails with Gradle #gradleVersion"() {
    given:
    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Failure.java')
    f.createNewFile()
    getClass().getResource("/test/Failure.java").withInputStream { f << it }

    when:
    def result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileJava')
        .buildAndFail()

    then:
    result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.FAILED
    result.output.contains("Failure.java:6: error: [ArrayEquals]")

    where:
    gradleVersion << IntegrationTestHelper.GRADLE_VERSIONS
  }
}
