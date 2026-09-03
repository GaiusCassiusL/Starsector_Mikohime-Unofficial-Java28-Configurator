# Mikohime Source Optimize

This directory reconstructs the contents of `../mikohime` as a reproducible,
verified multi-artifact build. It keeps verified upstream source separate from
source recovered from the bundled bytecode.

## Build

Prerequisites:

- Windows or 64-bit Linux
- JDK 21 in `JAVA_HOME`, or a local JDK at `.jdk\21`
- Network access on the first build so Gradle can resolve dependencies

Run:

```text
build.cmd       (Windows)
bash build.sh   (Linux)
```

The command performs a clean build, assembles the applicable distributions,
and runs verification. Both scripts prefer `.jdk/21` and fall back to
`JAVA_HOME`. Output is written to:

```text
build/dist/windows/
build/dist/linux/
```

Reports are written to:

```text
build/reports/artifact-status.json
build/reports/equivalence-report.json
build/reports/equivalence-report.txt
```

`artifact-status.json` is deterministic: if `SOURCE_DATE_EPOCH` is set, the
report records that value as `generatedAt`; otherwise it omits wall-clock time.

## Build strategies

| Artifacts | Strategy |
| --- | --- |
| `commons-compiler`, `commons-compiler-jdk`, `disruptor`, `janino`, `jaxb-api`, `log4j-api`, `log4j-plugins`, `txw2` | Resolved from Maven Central because their SHA-1 values exactly match the bundled files. |
| `jcraft-jorbis`, `jinput`, `lwjgl`, `lwjgl_util` | Recompiled from recovered Java source. `jcraft-jorbis` also has the original `module-info.class` reinserted during distribution staging so JPMS metadata matches the shipped jar. |
| `log4j-1.2-api`, `log4j-core` | The recovered custom `Log4jPlugins.java` descriptor is compiled and overlaid on the corresponding upstream JAR. Overlay assembly now fails if any class other than the intended plugin descriptor is produced or replaced. |
| `xstream-1.4.21_miko` | The recovered modified `XStream.java` and `Types.java` units are compiled and overlaid on XStream 1.4.21. Overlay assembly now fails if anything outside the seven intended XStream/Types class files is produced or replaced. |

Overlay builds avoid recompiling hundreds of unchanged decompiled classes while
preserving upstream manifests, module descriptors, services, and resources.
No original JAR is copied into the output as a fallback.

## Verification

The verifier now checks:

- the exact intended Windows Mikohime file set (15 JARs plus copied
  configuration files, resources, and natives), failing on unexpected
  extras as well as missing files;
- byte identity for all eight upstream-resolved artifacts;
- complete rebuilt class inventories and class metadata using ASM without
  comparing method bodies or debug info;
- public, protected, and package-private members plus private native methods,
  descriptors, declared exceptions, constants, annotations,
  inner/nest/permitted/record metadata, manifests, and
  `module-info.class` where present;
- per-artifact linkage by loading every rebuilt class without initialization in
  isolated dependency classloaders and resolving declared fields,
  constructors, methods, annotations, and record metadata;
- isolated runtime probes for JOrbis, JInput, LWJGL, LWJGL Util, both custom
  Log4j plugin descriptors, XStream serialization/deserialization, and LWJGL
  native initialization;
- Linux CI initialization of the packaged LWJGL and JInput JNI libraries.

Expected linkage exclusions are limited to classes whose signatures directly
reference optional dependencies that are not part of the intended flat-folder
runtime, plus the unavoidable platform-specific code paths that cannot be
resolved from the shipped Windows-only distribution:

- XStream optional adapters for Dom4J, JDOM/JDOM2, XOM, Jettison, XML Pull,
  MXParser/XPP3, CGLIB, Woodstox, Joda-Time, and the optional
  `javax.activation` / `javax.xml.bind` helpers;
- Log4j Core JSON/YAML, alternate async queue, OSGi, JAnsi, and Commons
  Compress integrations;
- Log4j 1.2 API's optional JMS renderer;
- LWJGL Util's optional ASM-backed mapped-object transformer support;
- LWJGL's macOS-only `com.apple.eio.FileManager` path, if reflective linkage on
  a non-macOS host requires it.

A successful platform build means these compatibility checks passed for all 15
JARs and copied ancillary files. A full Starsector launch and gameplay test is
still required because the game itself is not part of this source tree.

## Reproducibility controls

Gradle dependency locking is enabled for all projects, and the repository can
carry generated lockfiles plus `gradle/verification-metadata.xml` so dependency
versions and resolved checksums stay pinned to the verified build.

The Gradle wrapper is pinned to `gradle-8.14.3-bin.zip` with
`distributionSha256Sum=bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531`,
matching Gradle's official
`https://services.gradle.org/distributions/gradle-8.14.3-bin.zip.sha256`
checksum endpoint.

Every rebuilt `Jar` task now declares duplicate entries as a hard failure and
uses `isPreserveFileTimestamps = false` plus
`isReproducibleFileOrder = true`. The staging/distribution copy pipeline also
fails on duplicate destinations. Upstream-resolved exact-match jars are still
copied verbatim rather than repacked.

`verification` caches per-run `JarSnapshot` data by normalized jar path so the
metadata comparison and linkage phases reuse the same in-memory scan results
without holding `JarFile` handles open between checks.

## Layout

- `modules/` - reconstructed build modules, with custom Log4j and XStream overlays under `modules/overlays/`
- `reference/official-source/` - published source for the eight exact upstream matches
- `reference/original-binaries/` - original artifacts used for verification and compatibility metadata
- `../distribution/shared/` - shared Mikohime configuration and resources
- `../distribution/windows/` - Windows JVM template and original native DLLs
- `../distribution/linux/` - Linux JVM template
- `../configurator/` - shared configurator data plus CMD and Bash front ends
- `tooling/verification/` - ASM-based metadata/resource checks, linkage validation, and runtime probes
- `tooling/apple-stub/` - compile-time-only compatibility stub, never shipped

Recovered source is not the original authored source. Original comments,
formatting, some local names, and original build metadata cannot be recovered.
All upstream licenses and redistribution requirements continue to apply.
