package net.ltgt.gradle.errorprone
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class ErrorPronePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.apply plugin: ErrorProneBasePlugin
        def closure = { task ->
            task.javaCompiler = ErrorProneCompiler.createIncrementalCompiler(project.configurations.errorprone, task.outputs)
        }
        def javaCompileTasks = project.tasks.withType(JavaCompile);
        javaCompileTasks.each closure
        javaCompileTasks.whenTaskAdded(closure)
    }
}
