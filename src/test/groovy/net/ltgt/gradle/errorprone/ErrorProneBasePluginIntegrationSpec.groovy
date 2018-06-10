package net.ltgt.gradle.errorprone

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ErrorProneBasePluginIntegrationSpec extends Specification {
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
      apply plugin: 'net.ltgt.errorprone-base'
      apply plugin: 'java'

    """.stripIndent()

    def f = new File(testProjectDir.newFolder('src', 'main', 'java', 'test'), 'Success.java')
    f.createNewFile()
    getClass().getResource("/test/Success.java").withInputStream { f << it }
    f = new File(testProjectDir.newFolder('src', 'test', 'java', 'test'), 'Failure.java')
    f.createNewFile()
    getClass().getResource("/test/Failure.java").withInputStream { f << it }
  }

  private addErrorProneDependencies() {
    buildFile << """\
      dependencies {
        errorprone fileTree(\$/${System.getProperty('dependencies')}/\$)
      }
    """.stripIndent()
  }

  def "compilation succeeds even when code violates pattern, because Error Prone is not used"() {
    given:
    addErrorProneDependencies()

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileTestJava')
        .build()

    then:
    !result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS
    result.task(':compileTestJava').outcome == TaskOutcome.SUCCESS
  }

  def "compilation succeeds (Error Prone applied to compileJava only)"() {
    given:
    addErrorProneDependencies()
    buildFile << """
      import net.ltgt.gradle.errorprone.ErrorProneToolChain

      compileJava {
        toolChain ErrorProneToolChain.create(project)
      }
    """.stripIndent()

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileTestJava')
        .build()

    then:
    result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS
    result.task(':compileTestJava').outcome == TaskOutcome.SUCCESS
  }

  def "warns if errorprone dependencies not configured explicitly"() {
    given:
    // XXX: this uses an internal method…
    buildFile << """\
      task runErrorProneConfigurationDependencyActions {
        doLast {
          configurations.errorprone.runDependencyActions()
        }
      }
    """.stripIndent()

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('runErrorProneConfigurationDependencyActions')
        .build()

    then:
    result.output.contains(ErrorProneBasePlugin.WARNING_MESSAGE)
  }
}
