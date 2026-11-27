import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    // Formatting and compiler warnings, checked by `check`: https://github.com/Brendan-Black/black-java
    id("black.java-standards") version "1.3.6"
    // NullAway: whatever isn't @Nullable is never null, checked at compile time.
    id("net.ltgt.errorprone") version "5.2.0"
    // Minecraft to compile against, the graft configuration, runClient/runServer and gameTest.
    id("black.graft.mod") version "v0.1.2"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

group = "black.quickcreate"
version = "0.1.0"

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }

// Compiled against Graft's API; run with Graft's agent. Both come from JitPack, as for any Graft mod. 0.1.1 is the
// first Graft that reads side=server, which keeps QuickCreate off the join check: only the server needs it.
dependencies {
    compileOnly("com.github.Brendan-Black.Graft:api:v0.1.2")
    graft("com.github.Brendan-Black.Graft:loader:v0.1.2")
    testImplementation("com.github.Brendan-Black.Graft:api:v0.1.2")
    errorprone("com.google.errorprone:error_prone_core:2.53.0")
    errorprone("com.uber.nullaway:nullaway:0.14.3")
}
sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test { java.setSrcDirs(listOf("test")) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.add("-Xlint:all,-processing,-classfile")
    options.errorprone {
        enabled.set(this@configureEach.name == "compileJava")
        disableAllWarnings = true
        error("NullAway")
        option("NullAway:AnnotatedPackages", "black.quickcreate")
    }
}

val runTests = tasks.register<JavaExec>("runTests") {
    description = "Runs test/black/quickcreate/AllTests."
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "black.quickcreate.AllTests"
    workingDir = projectDir
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
}
tasks.test { enabled = false }
tasks.check { dependsOn(runTests) }

tasks.jar { from("LICENSE") }
