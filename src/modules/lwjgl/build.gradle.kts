// lwjgl: legacy LWJGL 2 native-binding library (OpenGL/OpenAL/OpenCL/input).
// Uses sun.misc.Unsafe (exported unconditionally by the jdk.unsupported
// module, resolvable without extra flags) and, on the macOS-only code path,
// com.apple.eio.FileManager (satisfied at compile time only by the tiny
// :tooling:apple-stub project; see that module's Javadoc).
//
// Because MemoryUtilSun/MacOSXSysImplementation reference JDK-internal /
// platform-only APIs, this module compiles against the actual installed JDK
// classes (sourceCompatibility/targetCompatibility) rather than `--release`,
// since `--release` uses a stripped public-API-only symbol table that does
// not include internal classes at all.
plugins {
    java
}

layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("module-builds/lwjgl"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly(project(":tooling:apple-stub"))
    // jinput provides net.java.games.input.* referenced by Controllers.java / JInputController.java.
    // Using the pre-built JAR to avoid jinput's task-ordering issue.
    compileOnly(files("../../reference/original-binaries/jinput.jar"))
}

// apple-stub is compiled with Java 21 toolchain but is compileOnly; relax the
// JVM target compatibility attribute check on compileClasspath so Gradle accepts
// it against our Java-8-targeted module.
configurations.named("compileClasspath") {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
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
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("lwjgl")
    archiveVersion.set("")
    archiveFileName.set("lwjgl.jar")
    // The legacy desktop distribution compiled against the OpenGL ES sources
    // but shipped only ContextAttribs from that package.
    eachFile {
        if (path.startsWith("org/lwjgl/opengles/")
            && path != "org/lwjgl/opengles/ContextAttribs.class") {
            exclude()
        }
    }
    manifest.from("META-INF/MANIFEST.MF")
}
