package net.ltgt.gradle.errorprone

import static org.junit.Assert.*

import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.internal.tasks.compile.CompilationFailedException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class ErrorPronePluginTest {
    @Test
    void shouldSucceed() {
        def project = ProjectBuilder.builder()
                .withProjectDir(new File('integrationTests/success'))
                .build();
        project.apply plugin: 'java'
        project.apply plugin: ErrorPronePlugin
        project.repositories {
            mavenCentral()
        }

        project.compileJava.execute()
        assertTrue(project.compileJava.didWork)
    }

    @Test
    void shouldFail() {
        def project = ProjectBuilder.builder()
                .withProjectDir(new File('integrationTests/failure'))
                .build();
        project.apply plugin: 'java'
        project.apply plugin: ErrorPronePlugin
        project.repositories {
            mavenCentral()
        }

        try {
            project.compileJava.execute()
            fail()
        } catch (TaskExecutionException tee) {
            assertTrue(tee.getCause() instanceof CompilationFailedException)
        }
    }
}
