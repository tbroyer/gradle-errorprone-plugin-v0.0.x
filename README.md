Gradle error-prone plugin v0.0.x
================================

## This repository contains the implementation for versions 0.0.x of the plugin, new development (starting with version 0.6) happens in https://github.com/tbroyer/gradle-errorprone-plugin

This plugin configures `JavaCompile` tasks to use the [error-prone compiler].

[error-prone compiler]: http://errorprone.info/

Requirements
------------

This plugin depends on Gradle internal APIs,
and is only guaranteed to work with the Gradle version it's been tested against.
It also depends on Javac internal APIs, directly exposed by error-prone.

 Gradle error-prone plugin version | Supported Gradle versions | Supported error-prone version | Supported javac version
 --------------------------------- | ------------------------- | ----------------------------- | -----------------------
 0.0.1, 0.0.2                      | 1.8 - 1.11                | 1.+                           | 7
 0.0.3                             | 1.8 - 1.12                | 1.+                           | 7
 0.0.4                             | 2.1                       | 1.+                           | 7
 0.0.5                             | 2.2 - 2.3                 | 1.+                           | 7
 0.0.6                             | 2.2 - 2.3                 | 1.+¹, 2.+                     | 7, 8
 0.0.7, 0.0.7.1                    | 2.4 - 2.5                 | 1.+¹, 2.+                     | 7, 8
 0.0.8                             | 2.6 - 3.4                 | 1.+¹, 2.+                     | 7, 8
 0.0.9                             | 2.6 - 3.4                 | 1.+¹, 2.+                     | 7, 8
 0.0.10                            | 2.6 - 4.9                 | 1.+¹, 2.+                     | 7, 8
 0.0.11                            | 2.6 - 4.9                 | 1.+¹, 2.+                     | 7, 8
 0.0.12                            | 2.6 - 4.9                 | 2.+                           | 8
 0.0.13                            | 2.6 - 4.9                 | 2.+                           | 8, 9
 0.0.14                            | 2.6 - 4.9                 | 2.+                           | 8, 9
 0.0.15                            | 2.6 - 4.9                 | 2.+                           | 8, 9
 0.0.16                            | 2.6 - 4.9                 | 2.+                           | 8, 9
 _master_                          | 2.6 - 4.9                 | 2.+                           | 8, 9

¹: error-prone 1.x is only supported with JDK 7

_Note: Gradle 2.0 is not supported;
it lacks APIs to manipulate the actual compiler being used.
Similarly, Gradle 3.5-rc-1 removed APIs this plugin uses,
they've been reintroduced in 3.5-rc-2._

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

When applied, the `net.ltgt.errorprone` plugin automatically changes all `JavaCompile` tasks in
the project to use the error-prone compiler.
(Note: earlier versions used `errorprone` as the plugin identifier instead of `net.ltgt.errorprone`.)

You can [configure the error-prone compiler](http://errorprone.info/docs/flags) using the `JavaCompile`'s `options.compilerArgs`,
for example:

```groovy
tasks.withType(JavaCompile) {
  // In Gradle 4.9+, prefer using tasks.withType(JavaCompile).configureEach {
  options.compilerArgs += [ '-Xep:DeadException:WARN', '-Xep:GuardedByValidator:OFF' ]
}
```

The plugin adds an `errorprone` configuration that automatically uses the latest release of error-prone.
You *should* override it to use a specific version with:

```groovy
dependencies {
  errorprone 'com.google.errorprone:error_prone_core:2.3.1'
}
```

<details>
<summary>or, for versions of the plugin before (and including) 0.0.8:</summary>

```groovy
configurations.errorprone {
  // 2.0.5 is the last version compatible with JDK 7
  resolutionStrategy.force 'com.google.errorprone:error_prone_core:2.0.5'
}
```

</details>

**WARNING:** Using a dynamic or changing version for Error Prone,
such as the default configuration using `latest.release`,
means that your build could fail at any time,
if a new version of Error Prone adds or enables new checks that your code would trigger.

If you forked error-prone and changed the `groupId`, use:

```groovy
dependencies {
  // Use my.company fork of error-prone
  errorprone "my.company.errorprone:error_prone_core:latest.release"
}
```

<details>
<summary>or, for versions of the plugin before (and including) 0.0.8:</summary>

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

</details>

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

  inputs.files(configurations.errorprone).withNormalizer(ClasspathNormalizer)
  // With Gradle < 4.3, use:
  // inputs.files(configurations.errorprone)
}
```

You can go further and provide a `configuration` containing the `com.google.errorprone:error_prone_core` dependency
(defaults to `configurations.errorprone`).
The above configuration is thus equivalent to:

```groovy
import net.ltgt.gradle.errorprone.ErrorProneToolChain

compileJava {
  toolChain new ErrorProneToolChain(configurations.errorprone)

  inputs.files(configurations.errorprone).withNormalizer(ClasspathNormalizer)
  // With Gradle < 4.3, use:
  // inputs.files(configurations.errorprone)
}
```

Troubleshooting
---------------

If your build fails with a compiler error,
before opening an issue here,
first make sure you're not suffering from a dependency misconfiguration,
or hitting an Error Prone bug.

### Dependency configuration issue

Run `gradle dependencies --configuration errorprone`
and check that your build doesn't mistakenly override any dependency version.

This could be the case if you're using the [Spring Dependency management plugin](https://plugins.gradle.org/plugin/io.spring.dependency-management),
or the [Nebula Resolution Rules Plugin](https://plugins.gradle.org/plugin/nebula.resolution-rules),
[Nebula Dependency Recommender](https://plugins.gradle.org/plugin/nebula.dependency-recommender),
or [Nebula Blacklist Plugin](https://plugins.gradle.org/plugin/nebula.blacklist).

### Error Prone bug

 1. Re-run your Gradle build with `--debug` and locate the compiler arguments in the outputs.  
    The plugin should output a line with `Compiling with error-prone compiler`;
    you can also search for `-sourcepath` in the output.
 2. Download the [standalone Error Prone compiler](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.google.errorprone%22%20AND%20a%3A%22error_prone_ant%22)
    (make sure you use the same version),
    and [run it](http://errorprone.info/docs/installation#command-line) with those arguments.  
    You might have to add an empty-string argument after `-sourcepath`
    (rendering of the arguments in Gradle output might omit the quotes for that empty string)

If those steps reproduce the issue, then it's an ErrorProne bug
(in which case please report the issue to [the Error Prone project](https://github.com/google/error-prone/issues));
otherwise it might be a gradle-errorprone-plugin bug (and then I'd need a repro case to debug what's happening).

You can also try using different versions of Error Prone.
If that fixes your problem, then again it likely is **not** a gradle-errorprone-plugin bug.
