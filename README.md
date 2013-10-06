Gradle error-prone plugin
=========================

This plugin configures `JavaCompile` tasks to use the [error-prone compiler].

[error-prone compiler]: https://code.google.com/p/error-prone/

Requirements
------------

This plugin depends on Gradle internal APIs, and only works with Gradle 1.8.

Usage
-----

To use the error-prone plugin, first add it to your project:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'net.ltgt.gradle:gradle-errorprone-plugin:latest.release'
  }
}

apply plugin: 'errorprone'
```

then make sure you have a `repository` configured that contains the
`com.google.errorprone:error_prone_core` dependency; for example:

```groovy
repositories {
  mavenCentral()
}
```

When applied, the `errorprone` plugin automatically  changes all `JavaCompile` tasks in
the project to use the error-prone compiler.

Advanced usage
--------------

If you want more control as to which task to change, you can apply the `errorprone-base`
plugin instead, which doesn't reconfigure any task. You'll then configure each task as
follows (using the `compileJava` task as an example):

```groovy
import net.ltgt.gradle.errorprone.ErrorProneCompiler

compileJava {
  javaCompiler ErrorProneCompiler.createIncrementalCompiler(delegate)
}

// Alternative:
// compileJava.javaCompiler ErrorProneCompiler.createIncrementalCompiler(compileJava)
```

You can go further and provide a `configuration` containing the
`com.google.errorprone:error_prone_core` dependency (defaults to 
`configurations.errorprone`) and task outputs (defaults to `task.outputs`). The above
configuration is thus equivalent to:

```groovy
compileJava {
  javaCompiler ErrorProneCompiler.createIncrementalCompiler(configurations.errorprone, outputs)
}
```
