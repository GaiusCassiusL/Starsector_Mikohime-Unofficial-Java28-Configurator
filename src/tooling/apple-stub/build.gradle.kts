// Minimal compile-time-only stub for the macOS-only com.apple.eio.FileManager
// class referenced by org.lwjgl.MacOSXSysImplementation. Never shipped; only
// ever used as a compileOnly dependency of the :modules:lwjgl module.
plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
