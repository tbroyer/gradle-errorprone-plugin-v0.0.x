package net.ltgt.gradle.errorprone

import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.TaskOutputsInternal
import org.gradle.api.internal.tasks.SimpleWorkResult
import org.gradle.api.internal.tasks.compile.CompilationFailedException
import org.gradle.api.internal.tasks.compile.Compiler
import org.gradle.api.internal.tasks.compile.IncrementalJavaCompiler
import org.gradle.api.internal.tasks.compile.JavaCompileSpec
import org.gradle.api.internal.tasks.compile.JavaCompilerArgumentsBuilder
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.WorkResult
import org.gradle.internal.jvm.Jvm

class ErrorProneCompiler implements Compiler<JavaCompileSpec> {
    private static final Logger LOGGER = Logging.getLogger(this)

    static Compiler<JavaCompileSpec> createIncrementalCompiler(Task task) {
        return createIncrementalCompiler(task.project.configurations.errorprone, task.outputs)
    }

    static Compiler<JavaCompileSpec> createIncrementalCompiler(Configuration configuration, TaskOutputsInternal taskOutputs) {
        return new IncrementalJavaCompiler(new ErrorProneCompiler(configuration), null, taskOutputs)
    }

    private Configuration errorprone;

    ErrorProneCompiler(Configuration errorprone) {
        this.errorprone = errorprone
    }

    @Override
    WorkResult execute(JavaCompileSpec spec) {
        LOGGER.info("Compiling with error-prone compiler")

        def args = new JavaCompilerArgumentsBuilder(spec).includeSourceFiles(true).build() as String[]

        def urls = [ Jvm.current().toolsJar.toURI().toURL() ]
        errorprone.each { f ->
            urls << f.toURI().toURL()
        }

        def cl = new URLClassLoader(urls as URL[], null as ClassLoader)
        def builderClass = cl.loadClass('com.google.errorprone.ErrorProneCompiler$Builder')

        def tccl = Thread.currentThread().contextClassLoader
        def result;
        try {
            Thread.currentThread().contextClassLoader = cl

            def compilerBuilder = builderClass.newInstance();
            def compiler = builderClass.getMethod("build").invoke(compilerBuilder)
            result = compiler.class.getMethod("compile", [ String[].class ] as Class[]).invoke(compiler, [ args ] as Object[])
        } finally {
            Thread.currentThread().contextClassLoader = tccl
        }

        if (result != 0) {
            throw new CompilationFailedException(result)
        }

        return new SimpleWorkResult(true);
    }
}
