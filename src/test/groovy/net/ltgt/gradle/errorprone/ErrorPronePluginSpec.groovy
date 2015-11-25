package net.ltgt.gradle.errorprone

import nebula.test.PluginProjectSpec
import org.gradle.api.tasks.compile.JavaCompile

class ErrorPronePluginSpec extends PluginProjectSpec {
  @Override String getPluginName() {
    return 'net.ltgt.errorprone'
  }


  def 'should apply errorprone-base plugin'() {
    when:
    project.apply plugin: pluginName
    project.evaluate()

    then:
    project.plugins.hasPlugin('errorprone-base')
    project.configurations.findByName('errorprone')
  }

  def 'should configure all JavaCompile tasks'() {
    when:
    project.apply plugin: pluginName
    project.apply plugin: 'java'
    project.evaluate()

    then:
    project.plugins.hasPlugin('errorprone-base')
    project.configurations.findByName('errorprone')
    project.tasks.withType(JavaCompile).all { it.toolChain instanceof ErrorProneToolChain }
  }
}
