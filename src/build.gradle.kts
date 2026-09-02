import java.time.Instant
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import org.gradle.api.GradleException
import org.gradle.language.jvm.tasks.ProcessResources

// ---------------------------------------------------------------------------
// Mikohime Source Code Test - reproducible multi-module rebuild
// ---------------------------------------------------------------------------
// This root build coordinates 15 independent artifacts that together
// reproduce the flat-folder contents of ../mikohime:
//   - 8 artifacts whose local binary is an exact SHA-1 match for a published
//     Maven Central artifact ("upstream-resolved"): these are NOT recompiled,
//     they are resolved from Maven Central and copied into the distribution
//     verbatim, because rebuilding verified-identical upstream source would
//     add no value (see README.md "Verified upstream source" table).
//   - 7 artifacts that differ from any known published binary ("rebuilt"):
//     these ARE compiled from the Vineflower-decompiled sources in
//     modules/<module> by dedicated subprojects declared in
//     settings.gradle.kts.
//
// Every subproject produces its jar directly under build/dist with the exact
// filename mikohime expects; official jars are fetched into the same
// directory by resolveOfficialJars. assembleDistribution then layers in the
// configuration/, resources/, and native/ trees so build/dist ends up a
// structural mirror of ../mikohime.
// ---------------------------------------------------------------------------

data class OfficialArtifact(
    val fileName: String,
    val coordinate: String,
)

data class RebuiltArtifact(
    val fileName: String,
    val projectPath: String,
    val reason: String,
)

val officialArtifacts = listOf(
    OfficialArtifact("commons-compiler-3.0.12.jar", "org.codehaus.janino:commons-compiler:3.0.12"),
    OfficialArtifact("commons-compiler-jdk-3.0.12.jar", "org.codehaus.janino:commons-compiler-jdk:3.0.12"),
    OfficialArtifact("disruptor-4.0.0.jar", "com.lmax:disruptor:4.0.0"),
    OfficialArtifact("janino-3.0.12.jar", "org.codehaus.janino:janino:3.0.12"),
    OfficialArtifact("jaxb-api-2.4.0-b180830.0359.jar", "javax.xml.bind:jaxb-api:2.4.0-b180830.0359"),
    OfficialArtifact("log4j-api-3.0.0-alpha1.jar", "org.apache.logging.log4j:log4j-api:3.0.0-alpha1"),
    OfficialArtifact("log4j-plugins-3.0.0-alpha1.jar", "org.apache.logging.log4j:log4j-plugins:3.0.0-alpha1"),
    OfficialArtifact("txw2-3.0.2.jar", "org.glassfish.jaxb:txw2:3.0.2"),
)

val rebuiltArtifacts = listOf(
    RebuiltArtifact(
        "jcraft-jorbis-0.0.17.jar",
        ":modules:jorbis",
        "Repacked/rebuilt binary that differs substantially from the Maven Central org.jcraft:jorbis release.",
    ),
    RebuiltArtifact(
        "jinput.jar",
        ":modules:jinput",
        "Unversioned legacy distribution with no confirmed exact upstream Maven binary.",
    ),
    RebuiltArtifact(
        "log4j-1.2-api-3.0.0-alpha1.jar",
        ":modules:overlays:log4j-1.2-api",
        "org/apache/log4j/plugins/Log4jPlugins.class differs from the Maven Central release (custom generated plugin descriptor).",
    ),
    RebuiltArtifact(
        "log4j-core-3.0.0-alpha1.jar",
        ":modules:overlays:log4j-core",
        "org/apache/logging/log4j/core/plugins/Log4jPlugins.class differs from the Maven Central release (custom generated plugin descriptor).",
    ),
    RebuiltArtifact(
        "lwjgl.jar",
        ":modules:lwjgl",
        "Unversioned legacy LWJGL 2 distribution with no confirmed exact upstream Maven binary.",
    ),
    RebuiltArtifact(
        "lwjgl_util.jar",
        ":modules:lwjgl-util",
        "Unversioned legacy LWJGL 2 utility distribution with no confirmed exact upstream Maven binary.",
    ),
    RebuiltArtifact(
        "xstream-1.4.21_miko.jar",
        ":modules:overlays:xstream",
        "Seven XStream/Types classes differ from the published com.thoughtworks.xstream:xstream:1.4.21 binary.",
    ),
)

fun jsonQuote(value: String): String = buildString {
    append('"')
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
    append('"')
}

fun readSourceDateEpoch(): Instant? {
    val raw = providers.environmentVariable("SOURCE_DATE_EPOCH").orNull?.trim().orEmpty()
    if (raw.isEmpty()) {
        return null
    }
    val seconds = raw.toLongOrNull()
        ?: throw GradleException("SOURCE_DATE_EPOCH must be an integer number of epoch seconds: $raw")
    require(seconds >= 0L) { "SOURCE_DATE_EPOCH must be non-negative: $raw" }
    return Instant.ofEpochSecond(seconds)
}

fun normalizedTimestamp(epochMillis: Long?, candidateMillis: Long): Long {
    return epochMillis ?: candidateMillis.takeIf { it >= 0L } ?: 0L
}

val sourceDateEpochInstant = readSourceDateEpoch()
val sourceDateEpochMillis = sourceDateEpochInstant?.toEpochMilli()
val distDir = layout.buildDirectory.dir("dist")
val officialStageDir = layout.buildDirectory.dir("staging/official")
val rebuiltStageDir = layout.buildDirectory.dir("staging/rebuilt")

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

subprojects {
    val isReconstructedJarProject = path.startsWith(":modules:")
    if (name != "verification") {
        repositories {
            mavenCentral()
        }
    }
    tasks.withType<ProcessResources>().configureEach {
        exclude("gradle.lockfile")
    }
    tasks.withType<Jar>().configureEach {
        exclude("gradle.lockfile")
        if (isReconstructedJarProject) {
            duplicatesStrategy = DuplicatesStrategy.FAIL
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
        }
    }
}

val officialConfiguration: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

repositories {
    mavenCentral()
}

dependencies {
    officialArtifacts.forEach { officialConfiguration(it.coordinate) }
}

val resolveOfficialJars by tasks.registering(Copy::class) {
    group = "mikohime"
    description = "Resolves the 8 exact-match artifacts from Maven Central and renames them to their mikohime filenames."
    duplicatesStrategy = DuplicatesStrategy.FAIL
    from(officialConfiguration) {
        officialArtifacts.forEach { artifact ->
            val (_, name, version) = artifact.coordinate.split(":")
            rename(Regex.escape("$name-$version.jar"), artifact.fileName)
        }
    }
    into(officialStageDir)
}

val preparedJcraftJorbisJar = layout.buildDirectory.file("prepared-rebuilt/jcraft-jorbis-0.0.17.jar")
val prepareJcraftJorbisJar by tasks.registering {
    group = "mikohime"
    description = "Reinserts the original module-info.class into the rebuilt jcraft-jorbis jar."
    val rebuiltJar = project(":modules:jorbis").tasks.named<Jar>("jar").flatMap { it.archiveFile }
    val originalJar = layout.projectDirectory.file("reference/original-binaries/jcraft-jorbis-0.0.17.jar")
    dependsOn(":modules:jorbis:jar")
    inputs.file(rebuiltJar)
    inputs.file(originalJar)
    inputs.property("sourceDateEpoch", providers.environmentVariable("SOURCE_DATE_EPOCH").orNull ?: "")
    outputs.file(preparedJcraftJorbisJar)
    doLast {
        val rebuiltFile = rebuiltJar.get().asFile
        val originalFile = originalJar.asFile
        val outputFile = preparedJcraftJorbisJar.get().asFile
        outputFile.parentFile.mkdirs()
        JarFile(rebuiltFile).use { rebuiltArchive ->
            val existingEntries = linkedSetOf<String>()
            val rebuiltEntries = rebuiltArchive.entries()
            while (rebuiltEntries.hasMoreElements()) {
                existingEntries += rebuiltEntries.nextElement().name
            }
            if ("module-info.class" in existingEntries) {
                rebuiltFile.copyTo(outputFile, overwrite = true)
                return@doLast
            }
            JarFile(originalFile).use { originalArchive ->
                val moduleEntry = originalArchive.getJarEntry("module-info.class")
                    ?: throw GradleException("reference/original-binaries/jcraft-jorbis-0.0.17.jar is missing module-info.class")
                JarOutputStream(outputFile.outputStream().buffered()).use { output ->
                    val entriesToCopy = rebuiltArchive.entries()
                    while (entriesToCopy.hasMoreElements()) {
                        val entry = entriesToCopy.nextElement()
                        if (entry.isDirectory) {
                            continue
                        }
                        val replacement = ZipEntry(entry.name)
                        replacement.time = normalizedTimestamp(sourceDateEpochMillis, entry.time)
                        output.putNextEntry(replacement)
                        rebuiltArchive.getInputStream(entry).use { input -> input.copyTo(output) }
                        output.closeEntry()
                    }
                    val injectedEntry = ZipEntry(moduleEntry.name)
                    injectedEntry.time = normalizedTimestamp(sourceDateEpochMillis, moduleEntry.time)
                    output.putNextEntry(injectedEntry)
                    originalArchive.getInputStream(moduleEntry).use { input -> input.copyTo(output) }
                    output.closeEntry()
                }
            }
        }
    }
}

val copyRebuiltJars by tasks.registering(Copy::class) {
    group = "mikohime"
    description = "Stages the 7 rebuilt artifacts for distribution assembly."
    duplicatesStrategy = DuplicatesStrategy.FAIL
    rebuiltArtifacts.forEach { artifact ->
        when (artifact.fileName) {
            "jcraft-jorbis-0.0.17.jar" -> {
                dependsOn(prepareJcraftJorbisJar)
                from(preparedJcraftJorbisJar)
            }
            else -> {
                dependsOn("${artifact.projectPath}:jar")
                from(project(artifact.projectPath).tasks.named<Jar>("jar"))
            }
        }
    }
    into(rebuiltStageDir)
}

val writeArtifactReport by tasks.registering {
    group = "mikohime"
    description = "Writes a deterministic machine-readable per-artifact status report."
    val reportFile = layout.buildDirectory.file("reports/artifact-status.json")
    inputs.property("sourceDateEpoch", providers.environmentVariable("SOURCE_DATE_EPOCH").orNull ?: "")
    outputs.file(reportFile)
    doLast {
        val reportEntries = buildList {
            officialArtifacts.forEach { artifact ->
                add(
                    listOf(
                        "      \"fileName\": ${jsonQuote(artifact.fileName)}",
                        "      \"status\": \"upstream-resolved\"",
                        "      \"sourceCompiled\": false",
                        "      \"coordinate\": ${jsonQuote(artifact.coordinate)}",
                        "      \"notes\": ${jsonQuote("Local binary SHA-1 matches Maven Central; resolved as verified dependency, not recompiled.")}",
                    ).joinToString(",\n", prefix = "    {\n", postfix = "\n    }"),
                )
            }
            rebuiltArtifacts.forEach { artifact ->
                val (status, notes) = when (artifact.fileName) {
                    "log4j-1.2-api-3.0.0-alpha1.jar",
                    "log4j-core-3.0.0-alpha1.jar",
                    "xstream-1.4.21_miko.jar" ->
                        "upstream-base-with-recompiled-custom-overlay" to
                            "${artifact.reason} Only the differing recovered source units are recompiled and overlaid on the upstream JAR; upstream manifests and non-overlay resources are preserved."
                    "jcraft-jorbis-0.0.17.jar" ->
                        "rebuilt-from-recovered-source" to
                            "${artifact.reason} The original module-info.class is reinserted during distribution staging so the rebuilt jar preserves upstream JPMS metadata."
                    else -> "rebuilt-from-recovered-source" to artifact.reason
                }
                add(
                    listOf(
                        "      \"fileName\": ${jsonQuote(artifact.fileName)}",
                        "      \"status\": ${jsonQuote(status)}",
                        "      \"sourceCompiled\": true",
                        "      \"gradleProject\": ${jsonQuote(artifact.projectPath)}",
                        "      \"notes\": ${jsonQuote(notes)}",
                    ).joinToString(",\n", prefix = "    {\n", postfix = "\n    }"),
                )
            }
        }.sortedBy { entry -> entry.substringAfter("\"fileName\": \"").substringBefore('"') }

        val headerLines = mutableListOf<String>()
        sourceDateEpochInstant?.let { instant ->
            headerLines += "  \"generatedAt\": ${jsonQuote(instant.toString())}"
            headerLines += "  \"generatedFrom\": \"SOURCE_DATE_EPOCH\""
        }
        headerLines += "  \"artifactCount\": ${officialArtifacts.size + rebuiltArtifacts.size}"
        headerLines += "  \"artifacts\": [\n${reportEntries.joinToString(",\n")}\n  ]"

        val output = reportFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText("{\n${headerLines.joinToString(",\n")}\n}\n")
    }
}

val assembleDistribution by tasks.registering(Sync::class) {
    group = "mikohime"
    description = "Builds the complete mikohime-equivalent output directory (15 jars + config/resources/native)."
    dependsOn(resolveOfficialJars, copyRebuiltJars, writeArtifactReport)
    duplicatesStrategy = DuplicatesStrategy.FAIL
    from(officialStageDir)
    from(rebuiltStageDir)
    from("distribution/configuration")
    from("distribution/resources")
    into(distDir)
    into("windows") {
        from("distribution/windows")
    }
    doLast {
        println("Distribution assembled at: ${distDir.get().asFile.absolutePath}")
    }
}

tasks.register("verify") {
    group = "mikohime"
    description = "Runs the metadata/resource/linkage verification tool comparing build/dist against reference binaries."
    dependsOn(":tooling:verification:run")
}
