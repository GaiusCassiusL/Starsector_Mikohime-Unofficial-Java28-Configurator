plugins {
    application
}

layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("module-builds/verification"))

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-tree:9.7.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("mikohime.verify.ArtifactVerifier")
}

tasks.named<JavaExec>("run") {
    dependsOn(rootProject.tasks.named("assembleWindowsDistribution"))
    args(
        rootProject.layout.projectDirectory.asFile.absolutePath,
        rootProject.layout.projectDirectory.dir("reference/original-binaries").asFile.absolutePath,
        rootProject.layout.buildDirectory.dir("dist/windows/mikohime").get().asFile.absolutePath,
        rootProject.layout.buildDirectory.file("reports/equivalence-report.json").get().asFile.absolutePath,
    )
}

tasks.register<JavaExec>("verifyLinuxNatives") {
    group = "verification"
    description = "Loads the packaged LWJGL and JInput JNI libraries on a Linux host."
    dependsOn(rootProject.tasks.named("assembleLinuxDistribution"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("mikohime.verify.LinuxNativeVerifier")
    args(rootProject.layout.buildDirectory.dir("dist/linux/mikohime").get().asFile.absolutePath)
    onlyIf {
        System.getProperty("os.name") == "Linux"
    }
}
