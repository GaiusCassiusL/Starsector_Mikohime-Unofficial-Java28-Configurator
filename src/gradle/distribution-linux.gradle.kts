import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar

val repositoryRoot = layout.projectDirectory.dir("..")
val distDir = layout.buildDirectory.dir("dist")
val officialStageDir = layout.buildDirectory.dir("staging/official")
val rebuiltStageDir = layout.buildDirectory.dir("staging/rebuilt")
val linuxNativeStageDir = layout.buildDirectory.dir("staging/linux-native")
val stagedLinuxConfigurator = layout.buildDirectory.file("staging/linux-configurator/Configure_Me.sh")
val stagedLinuxClasspath = layout.buildDirectory.file("staging/linux-configurator/classpath.entries")

val linuxLwjglNatives: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val linuxJinputNatives: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    linuxLwjglNatives("org.jmonkeyengine:lwjgl-platform:2.9.5:natives-linux")
    linuxJinputNatives("net.java.jinput:jinput:2.0.10:natives-all")
}

val extractLinuxNatives = tasks.register<Sync>("extractLinuxNatives") {
    group = "mikohime"
    description = "Extracts the verified 64-bit Linux LWJGL, OpenAL, and JInput JNI libraries."
    from({ linuxLwjglNatives.map(::zipTree) }) {
        include("liblwjgl64.so", "libopenal64.so")
    }
    from({ linuxJinputNatives.map(::zipTree) }) {
        include("libjinput-linux64.so")
    }
    into(linuxNativeStageDir)
    doLast {
        val expected = setOf("liblwjgl64.so", "libopenal64.so", "libjinput-linux64.so")
        val actual = linuxNativeStageDir.get().asFile.listFiles()
            ?.filter(File::isFile)
            ?.map(File::getName)
            ?.toSet()
            .orEmpty()
        if (actual != expected) {
            throw GradleException("Linux native set mismatch. Expected $expected, found $actual")
        }
    }
}

val prepareLinuxConfigurator = tasks.register("prepareLinuxConfigurator") {
    group = "mikohime"
    description = "Stages Linux runtime files with Unix LF line endings."
    val scriptSource = repositoryRoot.file("configurator/linux/Configure_Me.sh")
    val classpathSource = repositoryRoot.file("configurator/shared/classpath.entries")
    inputs.files(scriptSource, classpathSource)
    outputs.files(stagedLinuxConfigurator, stagedLinuxClasspath)
    doLast {
        listOf(
            scriptSource.asFile to stagedLinuxConfigurator.get().asFile,
            classpathSource.asFile to stagedLinuxClasspath.get().asFile,
        ).forEach { (source, output) ->
            output.parentFile.mkdirs()
            val normalized = source.readText(Charsets.UTF_8)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
            output.writeText(normalized, Charsets.UTF_8)
        }
    }
}

val assembleLinuxDistribution = tasks.register<Sync>("assembleLinuxDistribution") {
    group = "mikohime"
    description = "Builds the complete 64-bit Linux release directory."
    dependsOn(
        "resolveOfficialJars",
        "copyRebuiltJars",
        "writeArtifactReport",
        extractLinuxNatives,
        prepareLinuxConfigurator,
    )
    duplicatesStrategy = DuplicatesStrategy.FAIL
    into(distDir.map { it.dir("linux") })
    from(stagedLinuxConfigurator) {
        filePermissions {
            unix("rwxr-xr-x")
        }
    }
    from(repositoryRoot.files("README.md", "CHANGELOG.md"))
    into("mikohime") {
        from(officialStageDir)
        from(rebuiltStageDir)
        from(repositoryRoot.dir("distribution/shared/configuration"))
        from(repositoryRoot.dir("distribution/shared/resources"))
        from(repositoryRoot.dir("distribution/linux/configuration"))
        into("configurator/shared") {
            from(repositoryRoot.dir("configurator/shared")) {
                exclude("classpath.entries")
            }
            from(stagedLinuxClasspath)
        }
        into("linux") {
            from(linuxNativeStageDir)
        }
    }
    doLast {
        println("Linux distribution assembled at: ${distDir.get().dir("linux").asFile.absolutePath}")
    }
}

tasks.register<Tar>("packageLinuxDistribution") {
    group = "distribution"
    description = "Packages the Linux distribution with executable launchers."
    dependsOn(assembleLinuxDistribution)
    archiveFileName.set("Mikohime-linux-x64-v${project.version}.tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir("packages"))
    compression = Compression.GZIP
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from(distDir.map { it.dir("linux") })
    eachFile {
        mode = if (name == "Configure_Me.sh") {
            0b111101101
        } else {
            0b110100100
        }
    }
    dirMode = 0b111101101
}
