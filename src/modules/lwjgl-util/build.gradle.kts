// lwjgl_util: legacy LWJGL 2 utility library (vector/matrix math, GLU,
// mapped-object support, jinput bridge, WAV loading, and a bytecode
// verifier used by the mapped-object annotation processor).
//
// Depends on the reconstructed LWJGL and JInput modules.
// and on old "asm"-groupId ASM 3.3.1 artifacts, which is the last ASM release
// that still ships the org.objectweb.asm.ClassAdapter class the recovered
// source references (removed in later ASM majors). WaveData.java imports the
// JDK-internal, non-exported com.sun.media.sound.WaveFileReader class, which
// requires --add-exports even on the classic (non `--release`) compile path.
plugins {
    java
}

layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("module-builds/lwjgl_util"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    // Requires >= 9 because --add-exports (needed for the JDK-internal
    // com.sun.media.sound package below) is rejected by javac at target 8.
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":modules:lwjgl"))
    implementation(project(":modules:jinput"))
    compileOnly("asm:asm:3.3.1")
    compileOnly("asm:asm-tree:3.3.1")
    compileOnly("asm:asm-analysis:3.3.1")
    compileOnly("asm:asm-util:3.3.1")
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
    options.compilerArgs.addAll(listOf("--add-exports", "java.desktop/com.sun.media.sound=ALL-UNNAMED"))
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("lwjgl_util")
    archiveVersion.set("")
    archiveFileName.set("lwjgl_util.jar")
    manifest.from("META-INF/MANIFEST.MF")
}
