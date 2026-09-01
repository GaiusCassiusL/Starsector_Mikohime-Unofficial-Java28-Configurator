// jinput: legacy net.java.games.input controller-discovery API. The recovered
// source has no external (non-JDK) dependencies; native discovery libraries
// (jinput-dx8*.dll, jinput-raw*.dll) live under ../../distribution/windows and are
// loaded at runtime via JNI, not needed at compile time.
plugins {
    java
}

layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("module-builds/jinput"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    main {
        java.setSrcDirs(listOf("."))
        resources {
            setSrcDirs(listOf("."))
            exclude("**/*.java", "META-INF/MANIFEST.MF", "build.gradle.kts", "build/**", ".gradle/**")
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
    options.release.set(8)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("jinput")
    archiveVersion.set("")
    archiveFileName.set("jinput.jar")
    manifest.from("META-INF/MANIFEST.MF")
}
