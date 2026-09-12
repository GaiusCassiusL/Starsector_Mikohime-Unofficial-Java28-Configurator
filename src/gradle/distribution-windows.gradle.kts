import org.gradle.api.tasks.bundling.Zip

val repositoryRoot = layout.projectDirectory.dir("..")
val distDir = layout.buildDirectory.dir("dist")
val officialStageDir = layout.buildDirectory.dir("staging/official")
val rebuiltStageDir = layout.buildDirectory.dir("staging/rebuilt")

tasks.register<Sync>("assembleWindowsDistribution") {
    group = "mikohime"
    description = "Builds the complete Windows release directory."
    dependsOn("resolveOfficialJars", "copyRebuiltJars", "writeArtifactReport")
    duplicatesStrategy = DuplicatesStrategy.FAIL
    into(distDir.map { it.dir("windows") })
    from(repositoryRoot.file("configurator/windows/Configure_Me.cmd"))
    from(repositoryRoot.files("README.md", "CHANGELOG.md"))
    into("mikohime") {
        from(officialStageDir)
        from(rebuiltStageDir)
        from(repositoryRoot.dir("distribution/shared/configuration"))
        from(repositoryRoot.dir("distribution/shared/resources"))
        from(repositoryRoot.dir("distribution/windows/configuration"))
        into("configurator/shared") {
            from(repositoryRoot.dir("configurator/shared"))
        }
        into("configurator/windows") {
            from(repositoryRoot.dir("configurator/windows")) {
                exclude("Configure_Me.cmd")
            }
        }
        into("windows") {
            from(repositoryRoot.dir("distribution/windows/native"))
        }
    }
    doLast {
        println("Windows distribution assembled at: ${distDir.get().dir("windows").asFile.absolutePath}")
    }
}

tasks.register<Zip>("packageWindowsDistribution") {
    group = "distribution"
    description = "Packages the Windows distribution."
    dependsOn("assembleWindowsDistribution")
    archiveFileName.set("Mikohime-windows-x64-v${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("packages"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from(distDir.map { it.dir("windows") })
}
