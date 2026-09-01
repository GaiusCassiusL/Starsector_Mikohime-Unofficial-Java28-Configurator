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
    dependsOn(rootProject.tasks.named("assembleDistribution"))
    args(
        rootProject.layout.projectDirectory.asFile.absolutePath,
        rootProject.layout.projectDirectory.dir("reference/original-binaries").asFile.absolutePath,
        rootProject.layout.buildDirectory.dir("dist").get().asFile.absolutePath,
        rootProject.layout.buildDirectory.file("reports/equivalence-report.json").get().asFile.absolutePath,
    )
}
