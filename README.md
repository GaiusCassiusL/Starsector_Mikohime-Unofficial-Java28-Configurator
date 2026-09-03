# Mikohime Unofficial Java 28 Configurator

An unofficial, source-reconstructed Mikohime distribution and configurator for
running Starsector with Java 17, Java 27, or Java 28 on Windows and 64-bit Linux.
Redistribution of the Mikohime files is approved by the original mod author.

## Release packages

| Platform | Configurator | Generated launcher |
| --- | --- | --- |
| Windows x64 | `Configure_Me.cmd` | `Miko_Rouge.bat` |
| Linux x64 | `Configure_Me.sh` | `Miko_Rouge.sh` |

Extract the package into the Starsector installation directory. Keep
Starsector's bundled Java runtime; the configurator can also use a supported
local or system Java installation.

### Windows

1. Confirm `Configure_Me.cmd`, `mikohime`, and `starsector-core` are beside
   `starsector.exe`.
2. Run `Configure_Me.cmd`.
3. Select Java and the desired memory, CPU, logging, rendering, cache, and
   Prepatcher options.
4. Launch with `Miko_Rouge.bat`.

### Linux

1. Confirm `Configure_Me.sh`, `mikohime`, and `starsector-core` are beside
   `starsector.sh` or the `starsector` executable.
2. Make the configurator executable if the archive tool did not preserve its
   mode: `chmod +x Configure_Me.sh`.
3. Run `./Configure_Me.sh`, then launch with `./Miko_Rouge.sh`.

The Linux configurator detects local and system Java 17/27/28 installations,
selects a memory preset, detects Fast Rendering and related optional
components, and writes Linux-native paths and classpath separators. It can
install the pinned Adoptium builds with checksum validation:

```bash
./Configure_Me.sh --install-java 27
./Configure_Me.sh --install-java 28
```

Linux requires the normal Starsector desktop runtime libraries (X11, XRandR,
XCursor, XF86VidMode, OpenAL, and ALSA). Mods that use OpenCL, such as BoxUtil,
also require a working vendor or Mesa OpenCL ICD. `libOpenCL.so` is intentionally
not bundled because it must match the installed GPU driver.

## Java downloads

| Version | Windows | Linux |
| --- | --- | --- |
| Java 27 (`27+22-ea`) | [ZIP](https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27%2B22-ea-beta/OpenJDK-jdk_x64_windows_hotspot_27_22-ea.zip) | [tar.gz](https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27%2B22-ea-beta/OpenJDK-jdk_x64_linux_hotspot_27_22-ea.tar.gz) |
| Java 28 (`28+13-ea`) | [ZIP](https://github.com/adoptium/temurin28-binaries/releases/download/jdk-28%2B13-ea-beta/OpenJDK-jdk_x64_windows_hotspot_28_13-ea.zip) | [tar.gz](https://github.com/adoptium/temurin28-binaries/releases/download/jdk-28%2B13-ea-beta/OpenJDK-jdk_x64_linux_hotspot_28_13-ea.tar.gz) |

## Building

JDK 21 is required. Run `src\build.cmd` on Windows or `bash src/build.sh` on Linux.
Platform release roots are generated under:

```text
src/build/dist/windows/
src/build/dist/linux/
```

The Gradle build reconstructs 15 JARs, verifies the Windows-equivalent output,
and assembles platform-specific native libraries. Linux natives are
checksum-verified Maven artifacts:

- LWJGL/OpenAL: `org.jmonkeyengine:lwjgl-platform:2.9.5:natives-linux`
- JInput: `net.java.jinput:jinput:2.0.10:natives-all`

Linux CI loads both the reconstructed JARs and packaged JNI libraries to catch
ABI or native-loading failures. Tagged releases publish ZIP and tar.gz assets
through `.github/workflows/release.yml`.

## Repository layout

```text
configurator/
  shared/       Declarative classpath, memory, component, and Java data
  windows/      CMD configurator
  linux/        Bash configurator
distribution/
  shared/       Shared Mikohime configuration and resources
  windows/      Windows templates and DLL inputs
  linux/        Linux templates; native .so files are resolved by Gradle
src/            Reconstructed Java modules, references, build, and verification
```

## Disclaimer

This community package is not affiliated with Starsector, Fractal Softworks,
Adoptium, or the authors of optional third-party mods. Recovered source is not
the original authored source; original comments, formatting, and some local
names cannot be recovered.

The included Mikohime files are redistributed with permission from the mod author. Credit remains with the original modders and maintainers.

![Cane Toad (*Rhinella marina*), Border Ranges National Park](https://media.australian.museum/media/dd/images/Rhinella_marina_Border_Ranges_NP.width-1200.a2aaf34.jpg)
*“Cane Toad (Rhinella marina), Border Ranges NP” by Jodi Rowley / [Australian Museum](https://australian.museum/), used under its [Educational & Non-Commercial Terms](https://australian.museum/copyright/).*
