package net.ltgt.gradle.errorprone

import org.apache.commons.io.FileUtils
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
          classpath files('${System.getProperty('plugin')}')
        }
      }
      apply plugin: 'net.ltgt.errorprone'
      apply plugin: 'java'

      repositories {
        mavenCentral()
      }
      configurations.errorprone {
        // 2.0.5 is compatible with JDK 7 (2.0.6 is not)
        resolutionStrategy.force 'com.google.errorprone:error_prone_core:2.0.5'
      }
""".stripIndent()
  }

  @Unroll
  def "compilation succeeds with Gradle #gradleVersion"() {
    given:
    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Success.java')
    f.createNewFile()
    FileUtils.copyURLToFile(getClass().getResource("/test/Success.java"), f)

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
    gradleVersion << ['2.6', '2.7', '2.8', '2.9', '2.10']
  }

  @Unroll
  def "compilation fails with Gradle #gradleVersion"() {
    given:
    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Failure.java')
    f.createNewFile()
    FileUtils.copyURLToFile(getClass().getResource("/test/Failure.java"), f)

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
    gradleVersion << ['2.6', '2.7', '2.8', '2.9', '2.10']
  }

  def "compatible with errorprone 1.x"() {
    assumeFalse("errorprone 1.x as deployed to Central only supports Java 7",
        Jvm.current().getJavaVersion().isJava8Compatible());

    given:
    buildFile << """\
      configurations.errorprone.resolutionStrategy {
        force 'com.google.errorprone:error_prone_core:1.1.2'
      }
    """.stripIndent()

    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Success.java')
    f.createNewFile()
    FileUtils.copyURLToFile(getClass().getResource("/test/Success.java"), f)

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileJava')
        .build()

    then:
    result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS
  }
}
