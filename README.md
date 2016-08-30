Gradle error-prone plugin
=========================

This plugin configures `JavaCompile` tasks to use the [error-prone compiler].

[error-prone compiler]: http://errorprone.info/

Requirements
------------

This plugin depends on Gradle internal APIs,
and is only guaranteed to work with the Gradle version it's been compiled against
(check the `wrapper` task in the `build.gradle`.)
It also depends on Javac internal APIs, directly exposed by error-prone.

 Gradle error-prone plugin version | Supported Gradle versions | Supported error-prone version | Supported javac version
 --------------------------------- | ------------------------- | ----------------------------- | -----------------------
 0.0.1, 0.0.2                      | 1.8 - 1.11                | 1.+                           | 7
 0.0.3                             | 1.8 - 1.12                | 1.+                           | 7
 0.0.4                             | 2.1                       | 1.+                           | 7
 0.0.5                             | 2.2 - 2.3                 | 1.+                           | 7
 0.0.6                             | 2.2 - 2.3                 | 1.+¹, 2.+                     | 7, 8
 0.0.7, 0.0.7.1                    | 2.4 - 2.5                 | 1.+¹, 2.+                     | 7, 8
 0.0.8                             | 2.6 - 2.14, 3.0           | 1.+¹, 2.+                     | 7, 8
 _master_                          | 2.6 - 2.14, 3.0           | 1.+¹, 2.+                     | 7, 8

¹: error-prone 1.x is only supported with JDK 7

_Note: Gradle 2.0 is not supported; it lacks APIs to manipulate the actual
compiler being used._

Usage
-----

To use the error-prone plugin,
first add it to your project following the instructions from the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/net.ltgt.errorprone)¹,
then make sure you have a `repository` configured that contains the `com.google.errorprone:error_prone_core` dependency;
for example:

```groovy
repositories {
  mavenCentral()
}
```

¹: For older versions of the plugins, please read [the according version of this README](https://github.com/tbroyer/gradle-errorprone-plugin/commits/master/README.md)

When applied, the `net.ltgt.errorprone` plugin automatically  changes all `JavaCompile` tasks in
the project to use the error-prone compiler.
(Note: earlier versions used `errorprone` as the plugin identifier instead of `net.ltgt.errorprone`.)

The plugin adds an `errorprone` configuration that automatically uses the latest release of error-prone.
You can override it to use a specific version with:

```groovy
dependencies {
  // 2.0.5 is the last version compatible with JDK 7
  errorprone 'com.google.errorprone:error_prone_core:2.0.5'
}
```

or, for versions of the plugin before (and including) 0.0.8:

```groovy
configurations.errorprone {
  // 2.0.5 is the last version compatible with JDK 7
  resolutionStrategy.force 'com.google.errorprone:error_prone_core:2.0.5'
}
```

If you forked error-prone and changed the `groupId`, the syntax may vary (depending on the version of the plugin):

```groovy
dependencies {
  // Use my.company fork of error-prone
  errorprone "my.company.errorprone:error_prone_core:latest.release"
}
```

or

```groovy
// Use my.company fork of error-prone
configurations.errorprone {
  resolutionStrategy.eachDependency { DependencyResolveDetails details ->
    if (details.requested.group == 'com.google.errorprone') {
      details.useTarget "my.company.errorprone:${details.requested.name}:latest.release"
    }
  }
}
```

Advanced usage
--------------

If you want more control as to which task to change,
you can apply the `net.ltgt.errorprone-base` plugin instead,
which doesn't reconfigure any task.
You'll then configure each task as follows
(using the `compileJava` task as an example):

```groovy
import net.ltgt.gradle.errorprone.ErrorProneToolChain

compileJava {
  toolChain ErrorProneToolChain.create(project)
}
```

You can go further and provide a `configuration` containing the `com.google.errorprone:error_prone_core` dependency
(defaults to `configurations.errorprone`).
The above configuration is thus equivalent to:

```groovy
import net.ltgt.gradle.errorprone.ErrorProneToolChain

compileJava {
  toolChain new ErrorProneToolChain(configurations.errorprone)
}
```
