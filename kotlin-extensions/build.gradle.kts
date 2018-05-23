plugins {
    `kotlin-dsl`
}

val compileJava: JavaCompile by project(":").tasks.getting

dependencies {
    compile(files(compileJava.destinationDir).builtBy(compileJava))
}
