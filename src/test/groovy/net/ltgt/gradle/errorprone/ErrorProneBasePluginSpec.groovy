package net.ltgt.gradle.errorprone

import nebula.test.PluginProjectSpec
import org.gradle.api.tasks.compile.JavaCompile

class ErrorProneBasePluginSpec extends PluginProjectSpec {
  @Override String getPluginName() {
    return 'net.ltgt.errorprone-base'
  }

  def 'plugin should add errorprone configuration'() {
    when:
    project.apply plugin: pluginName
    project.evaluate()

    then:
    project.configurations.findByName('errorprone')
  }

  def 'plugin should not configure JavaCompile tasks'() {
    when:
    project.apply plugin: pluginName
    project.apply plugin: 'java'
    project.evaluate()

    then:
    project.configurations.findByName('errorprone')
    !project.tasks.withType(JavaCompile).any { it.toolChain instanceof ErrorProneToolChain }
  }
}
