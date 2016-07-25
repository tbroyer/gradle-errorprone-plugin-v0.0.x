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

      repositories {
        mavenCentral()
      }
      configurations.errorprone {
        // 2.0.5 is compatible with JDK 7 (2.0.6 is not)
        resolutionStrategy.force 'com.google.errorprone:error_prone_core:2.0.5'
      }
""".stripIndent()

    def p = testProjectDir.newFolder('src', 'main', 'java', 'test')
    def f = new File(p, 'Success.java')
    f.createNewFile()
    getClass().getResource("/test/Success.java").withInputStream { f << it }
    f = new File(p, 'Failure.java')
    f.createNewFile()
    getClass().getResource("/test/Failure.java").withInputStream { f << it }
  }

  def "compilation succeeds even when code violates pattern, because Error Prone is not used"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileJava')
        .build()

    then:
    !result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS
  }

  def "compilation succeeds (Error Prone applied to compileJava only)"() {
    given:
    buildFile << """
      import net.ltgt.gradle.errorprone.ErrorProneToolChain

      compileJava {
        include 'test/Success.java'
        toolChain ErrorProneToolChain.create(project)
      }
      task compileFailure(type: JavaCompile) {
        classpath = sourceSets.main.compileClasspath
        destinationDir = sourceSets.main.output.classesDir
        source sourceSets.main.java
        include 'test/Failure.java'
      }
    """.stripIndent()

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileJava', 'compileFailure')
        .build()

    then:
    result.output.contains("Compiling with error-prone compiler")
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS
    result.task(':compileFailure').outcome == TaskOutcome.SUCCESS
  }
}
