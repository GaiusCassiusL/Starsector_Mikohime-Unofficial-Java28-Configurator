# Mikohime Unofficial Java 28 Configurator

An unofficial, source-reconstructed Mikohime distribution and configurator for
running Starsector with Java 17, Java 27, or Java 28 on Windows. Experimental
64-bit Linux support is under development. Redistribution of the Mikohime files
is approved by the original mod author.

## Released platform

| Platform | Configurator | Generated launcher |
| --- | --- | --- |
| Windows x64 | `Configure_Me.cmd` | `Miko_Rouge.bat` |

Extract the package into the Starsector installation directory. Keep
Starsector's bundled Java runtime; the configurator can also use a supported
local or system Java installation.

### Windows

1. Confirm `Configure_Me.cmd` and `mikohime` folder are inside the same directory as
   `starsector.exe`.
2. Run `Configure_Me.cmd`. If Starsector is under `Program Files` or
   `Program Files (x86)`, right-click it and select **Run as administrator**.
3. Select a Java version and the desired memory, CPU, logging, rendering, cache, and
   Prepatcher options.
4. Launch with `Miko_Rouge.bat`.If Starsector is under `Program Files` or
   `Program Files (x86)`, right-click it and select **Run as administrator**.

If an automatic Java download fails, the configurator displays the trusted
Adoptium URL and can open it in your browser for manual installation.

### Experimental Linux support

> [!WARNING]
> Linux support is still in development, don't be surprised if you encounter bugs or issues. Its build and JNI initialization pass in
> CI, but it has not completed real Starsector launch, gameplay, mod, GPU, and
> distribution testing. Do not treat the Linux build as a supported release.

1. Confirm `Configure_Me.sh` and `mikohime` are in the Starsector installation
   directory beside `starsector.sh`. 
2. Open a terminal in the Starsector installation directory and run
   `./Configure_Me.sh`.
3. Launch the generated `Miko_Rouge.sh`.

The Linux configurator offers a guided menu and a seven-step flow with
back/cancel like the Windows configurator. It detects Java 17/27/28 installations, memory,
CPU cores, and huge-page capability; selects a memory preset or custom heap;
detects Fast Rendering, FR Resource Cache, StarsectorPrepatcher, and VRAM
Optimizer; tunes low-core and old-CPU options; picks a logging mode and launcher
background; and writes Linux-native paths and `:`-separated classpaths. All
outputs are validated and committed transactionally with rollback. It can
install the pinned Adoptium builds with checksum validation:

```bash
./Configure_Me.sh --install-java 27
./Configure_Me.sh --install-java 28
```

Non-interactive runs are deterministic through `MIKO_*` environment variables
(for example `MIKO_JAVA`, `MIKO_HEAP_MIB`, `MIKO_FAST_RENDERING`,
`MIKO_RESOURCE_CACHE`, `MIKO_PREPATCHER`, `MIKO_LOW_CORE`, `MIKO_OLD_CPU`,
`MIKO_LARGE_PAGES`, `MIKO_LOGGING`, and `MIKO_BACKGROUND`):

```bash
MIKO_JAVA=jdk-28+13/bin/java MIKO_HEAP_MIB=8192 ./Configure_Me.sh --non-interactive
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
Platform build roots are generated under:

```text
src/build/dist/windows/
src/build/dist/linux/
```

The Gradle build reconstructs 15 JARs, verifies the Windows-equivalent output,
and assembles platform-specific native libraries. Linux natives are
checksum-verified Maven artifacts:

- LWJGL/OpenAL: `org.jmonkeyengine:lwjgl-platform:2.9.5:natives-linux`
- JInput: `net.java.jinput:jinput:2.0.10:natives-all`

Packaged configurator metadata is kept under `mikohime/configurator/shared` to
avoid adding another directory to the Starsector installation root.

Linux CI loads both the reconstructed JARs and packaged JNI libraries to catch
ABI or native-loading failures. This is a development check and does not prove
that Starsector or third-party mods work correctly on Linux.

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
