import java.io.File
import java.util.jar.JarFile
import java.util.jar.Manifest
import org.gradle.api.GradleException

// Only the generated Log4jPlugins class differs from the published artifact.
// Compile that recovered source and overlay it on the verified upstream JAR.
plugins {
    java
}

layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("module-builds/log4j-1.2-api-3.0.0-alpha1"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val upstream by configurations.creating {
    isTransitive = false
}

val allowedOverlayEntries = setOf(
    "org/apache/log4j/plugins/Log4jPlugins.class",
)

fun compiledOverlayEntries(): Set<String> = sourceSets.main.get().output.classesDirs.files
    .filter(File::exists)
    .flatMap { root ->
        root.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .map { it.relativeTo(root).path.replace(File.separatorChar, '/') }
            .toList()
    }
    .toSortedSet()

dependencies {
    upstream("org.apache.logging.log4j:log4j-1.2-api:3.0.0-alpha1")
    compileOnly("org.apache.logging.log4j:log4j-1.2-api:3.0.0-alpha1")
    compileOnly("org.apache.logging.log4j:log4j-plugins:3.0.0-alpha1")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("org/apache/log4j/plugins"))
        java.include("Log4jPlugins.java")
        resources.setSrcDirs(emptyList<String>())
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
    options.release.set(11)
}

val validateOverlayAssembly by tasks.registering {
    group = "verification"
    description = "Fails if the Log4j 1.2 overlay compiles or replaces anything beyond the intended plugin descriptor."
    dependsOn(tasks.named("classes"))
    inputs.files(sourceSets.main.get().output.classesDirs)
    inputs.file(provider { upstream.singleFile })
    inputs.file(layout.projectDirectory.file("META-INF/MANIFEST.MF"))
    doLast {
        val compiledEntries = compiledOverlayEntries()
        val unexpectedEntries = compiledEntries - allowedOverlayEntries
        val missingEntries = allowedOverlayEntries - compiledEntries
        require(unexpectedEntries.isEmpty() && missingEntries.isEmpty()) {
            "Unexpected Log4j 1.2 overlay classes. Missing=$missingEntries, unexpected=$unexpectedEntries"
        }
        JarFile(upstream.singleFile).use { upstreamJar ->
            val upstreamEntries = upstreamJar.entries().asSequence().map { it.name }.toSet()
            val absentFromUpstream = allowedOverlayEntries - upstreamEntries
            if (absentFromUpstream.isNotEmpty()) {
                throw GradleException("Upstream Log4j 1.2 jar is missing overlay targets: $absentFromUpstream")
            }
            val upstreamManifest = upstreamJar.manifest
                ?: throw GradleException("Upstream Log4j 1.2 jar is missing META-INF/MANIFEST.MF")
            val localManifest = layout.projectDirectory.file("META-INF/MANIFEST.MF").asFile
                .inputStream().use(::Manifest)
            if (upstreamManifest.mainAttributes != localManifest.mainAttributes ||
                upstreamManifest.entries != localManifest.entries
            ) {
                throw GradleException("Recovered Log4j 1.2 manifest diverges from the upstream jar manifest.")
            }
        }
    }
}

tasks.named<Jar>("jar") {
    dependsOn(validateOverlayAssembly)
    archiveBaseName.set("log4j-1.2-api")
    archiveVersion.set("")
    archiveFileName.set("log4j-1.2-api-3.0.0-alpha1.jar")
    duplicatesStrategy = DuplicatesStrategy.FAIL
    from({
        zipTree(upstream.singleFile)
    }) {
        exclude(
            *allowedOverlayEntries.toTypedArray(),
            "META-INF/MANIFEST.MF",
        )
    }
    manifest.from("META-INF/MANIFEST.MF")
}
