pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "mikohime-rebuild"

// The seven artifacts recompiled from Vineflower-decompiled source.
include(
    ":modules:jorbis",
    ":modules:jinput",
    ":modules:lwjgl",
    ":modules:lwjgl-util",
    ":modules:overlays:log4j-1.2-api",
    ":modules:overlays:log4j-core",
    ":modules:overlays:xstream",
)

// Standalone verification tool (ASM-based API/resource diff + behavioral smoke tests).
include(":tooling:verification")

// Tiny compile-time-only stub for the macOS-only com.apple.eio.FileManager
// class referenced by the recovered LWJGL source. Never shipped.
include(":tooling:apple-stub")
