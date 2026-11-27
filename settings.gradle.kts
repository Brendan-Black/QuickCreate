pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    // JitPack serves Graft's plugin and black-java's by their repositories' names.
    resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("black.graft.")) useModule("com.github.Brendan-Black.Graft:gradle-plugin:${requested.version}")
        if (requested.id.id == "black.java-standards") useModule("com.github.Brendan-Black.black-java:plugin:${requested.version}")
    }
}

plugins {
    // Downloads the JDK 25 toolchain when the JDK running Gradle is older.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "QuickCreate"
