Gradle error-prone plugin
=========================

This plugin configures `JavaCompile` tasks to use the [error-prone compiler].

[error-prone compiler]: http://errorprone.info/

Requirements
------------

This plugin depends on Gradle internal APIs, and is only guaranteed to work
with the Gradle version it's been compiled against (check the `wrapper` task
in the `build.gradle`.)
It also depends on Javac internal APIs, directly exposed by error-prone.

 Gradle error-prone plugin version | Supported Gradle versions | Supported error-prone version | Supported javac version
 --------------------------------- | ------------------------- | ----------------------------- | -----------------------
 0.0.1, 0.0.2                      | 1.8 - 1.11                | 1.+                           | 7
 0.0.3                             | 1.8 - 1.12                | 1.+                           | 7
 0.0.4                             | 2.1                       | 1.+                           | 7
 0.0.5                             | 2.2 - 2.3                 | 1.+                           | 7
 0.0.6                             | 2.2 - 2.3                 | 1.+¹, 2.+                     | 7, 8
 0.0.7-SNAPSHOT                    | 2.2 - 2.3                 | 1.+¹, 2.+                     | 7, 8

¹: error-prone 1.x is only supported with JDK 7

_Note: Gradle 2.0 is not supported; it lacks APIs to manipulate the actual
compiler being used._

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

The plugin adds an `errorprone` configuration that automatically uses the latest
release of error-prone. You can override it to use a specific version with:

```groovy
configurations.errorprone {
  resolutionStrategy.force 'com.google.errorprone:error_prone_core:1.0.8-patched'
}
```

or if you changed the groupId of your fork:

```groovy
configurations.all {
  resolutionStrategy.eachDependency { DependencyResolveDetails details ->
    if (details.requested.group == 'com.google.errorprone') {
      details.useTarget "my.company.errorprone:${details.requested.name}:latest.release"
    }
  }
}
```

Advanced usage
--------------

If you want more control as to which task to change, you can apply the `errorprone-base`
plugin instead, which doesn't reconfigure any task. You'll then configure each task as
follows (using the `compileJava` task as an example):

```groovy
import net.ltgt.gradle.errorprone.ErrorProneToolChain

compileJava {
  toolChain ErrorProneToolChain.create(project)
}
```

You can go further and provide a `configuration` containing the
`com.google.errorprone:error_prone_core` dependency (defaults to
`configurations.errorprone`) The above configuration is thus equivalent to:

```groovy
import net.ltgt.gradle.errorprone.ErrorProneToolChain

compileJava {
  toolChain new ErrorProneToolChain.create(configurations.errorprone)
}
```

Advanced usage, legacy (Gradle 1.x)
-----------------------------------

```groovy
import net.ltgt.gradle.errorprone.ErrorProneCompiler

compileJava {
  javaCompiler ErrorProneCompiler.createIncrementalCompiler(delegate)
}

// Alternative:
// compileJava.javaCompiler ErrorProneCompiler.createIncrementalCompiler(compileJava)
```

For even more control, provide a `configuration` containing the
`com.google.errorprone:error_prone_core` dependency (defaults to
`configurations.errorprone`) and task outputs (defaults to `task.outputs`). The above
configuration is thus equivalent to:

```groovy
compileJava {
  javaCompiler ErrorProneCompiler.createIncrementalCompiler(configurations.errorprone, outputs)
}
```
