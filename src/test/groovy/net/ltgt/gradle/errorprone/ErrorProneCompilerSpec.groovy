package net.ltgt.gradle.errorprone

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ErrorProneCompilerSpec extends Specification {

    @Rule final TemporaryFolder testDir = new TemporaryFolder()

    File emptyJar

    void setup() {
        emptyJar = testDir.newFile("empty.jar")
    }

    void cleanup() {
        ErrorProneCompiler.SelfFirstClassLoader.INSTANCE = null
    }

    def "getInstance returns cached ClassLoader on call with same jars"() {
        given:
        def errorProneJars = new ErrorProneCompiler.ErrorProneJars(Collections.emptySet())
        def classLoader1 = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars)

        when:
        def classLoader2 = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars)

        then:
        classLoader1 == classLoader2
    }

    def "getInstance returns new ClassLoader on call with different jars"() {
        given:
        def errorProneJars1 = new ErrorProneCompiler.ErrorProneJars(Collections.emptySet())
        def errorProneJars2 = new ErrorProneCompiler.ErrorProneJars(Collections.singleton(emptyJar.toURI()))

        when:
        def classLoader1 = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars1)
        def classLoader2 = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars2)

        then:

        classLoader1 != classLoader2
    }

    def "getInstance sets ref count to 2 for new instance"() {
        given:
        def errorProneJars = new ErrorProneCompiler.ErrorProneJars(Collections.emptySet())

        when:
        def classLoader = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars)

        then:
        classLoader.refCount.get() == 2
    }

    def "getInstance increments ref count for cached instance"() {
        given:
        def errorProneJars = new ErrorProneCompiler.ErrorProneJars(Collections.emptySet())
        def classLoader = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars)

        when:
        ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars)

        then:
        classLoader.refCount.get() == 3
    }

    def "getInstance decrements ref count for previous instance on call with different jars"() {
        given:
        def errorProneJars1 = new ErrorProneCompiler.ErrorProneJars(Collections.emptySet())
        def errorProneJars2 = new ErrorProneCompiler.ErrorProneJars(Collections.singleton(emptyJar.toURI()))
        def classLoader1 = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars1)
        classLoader1.decrementRefCount()

        when:
        def classLoader2 = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars2)

        then:
        classLoader1.refCount.get() == 0
    }

    def "decrementRefCount decrements ref count"() {
        given:
        def errorProneJars = new ErrorProneCompiler.ErrorProneJars(Collections.emptySet())
        def classLoader = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars)

        when:
        classLoader.decrementRefCount()

        then:
        classLoader.refCount.get() == 1
    }

    def "decrementRefCount throws if below zero"() {
        given:
        def errorProneJars = new ErrorProneCompiler.ErrorProneJars(Collections.emptySet())
        def classLoader = ErrorProneCompiler.SelfFirstClassLoader.getInstance(errorProneJars)

        when:
        classLoader.decrementRefCount()
        classLoader.decrementRefCount()
        classLoader.decrementRefCount()

        then:
        thrown(IllegalStateException)
    }
}
