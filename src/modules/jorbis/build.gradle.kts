// jcraft-jorbis-0.0.17: pure-Java Ogg/Vorbis decoder (com.jcraft.jogg + com.jcraft.jorbis).
// The recovered source has no external (non-JDK) dependencies. The original
// binary is a JPMS-modular jar (it ships a module-info.class); module-info.java
// is intentionally excluded here rather than wired onto the module path,
// because Starsector loads mods from the classpath, not the module path, so a
// module descriptor has no effect on runtime behavior in this deployment.
// See README.md "Known limitations" for details; this is reported explicitly
// by the verification tool rather than silently dropped.
plugins {
    java
}

layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("module-builds/jcraft-jorbis-0.0.17"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("."))
        java.exclude("module-info.java")
        resources {
            setSrcDirs(listOf("."))
            exclude("**/*.java", "META-INF/MANIFEST.MF", "build.gradle.kts", "build/**", ".gradle/**")
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
    options.release.set(10)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("jcraft-jorbis")
    archiveVersion.set("")
    archiveFileName.set("jcraft-jorbis-0.0.17.jar")
    manifest.from("META-INF/MANIFEST.MF")
}
