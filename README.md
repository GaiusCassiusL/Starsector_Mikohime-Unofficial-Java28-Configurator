# Mikohime Unofficial Java 28 Configurator

An unofficial Mikohime source project and configurator that add Java 28 early-access support to Starsector.

Published release packages include all required Mikohime files with approval from the mod author (thanks, Yue). The original Mikohime package does not need to be downloaded or installed separately.

The configurator generates:

- `Miko_Rouge.bat`
- `Miko_Simple.txt`
- `Miko_Info.txt`

> [!IMPORTANT]
> Currently adds support for Java 28 builds `28+11-ea`, `28+12-ea`, and `28+13-ea`.

## Requirements

- A Windows installation of Starsector
- One of the supported Java versions

## Java Downloads

| Version | Download |
| --- | --- |
| Java 27 (`27+22-ea`) | [Download from Adoptium](https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27%2B22-ea-beta/OpenJDK-jdk_x64_windows_hotspot_27_22-ea.zip) |
| Java 28 (`28+13-ea`) | [Download from Adoptium](https://github.com/adoptium/temurin28-binaries/releases/download/jdk-28%2B13-ea-beta/OpenJDK-jdk_x64_windows_hotspot_28_13-ea.zip) |

~ Just use `28+13-ea` ~

## Installation

1. Download this package and extract its contents into your Starsector directory beside `starsector.exe`.

   ```text
   .\Starsector
   ```

2. Keep Starsector's bundled `jre` folder. Do not delete or replace it.

3. To use Java 27 or Java 28, download and extract the desired JDK into a separate folder beside `starsector.exe` or choose to download and install it directly from within `Configure_Me.cmd`.

   Example:

   ```text
   .\Starsector\jdk-27+22
   .\Starsector\jdk-28+13
   ```

4. Run `Configure_Me.cmd`.

   > If Starsector is installed under `Program Files`, you may need to run the configurator as an administrator.

5. Select a detected Java installation and configure the available memory, CPU, logging, Large Pages, Fast Rendering, FR Resource Cache, and StarsectorPrepatcher options.

6. Launch Starsector using the generated `Miko_Rouge.bat`.

## Building Mikohime

The reconstructed source and build system are under `src`. From that directory,
run `build.cmd` with JDK 21 configured through `JAVA_HOME` or installed locally
at `src\.jdk\21`. The verified distribution is generated under
`src\build\dist`.

## Troubleshooting

Run `Configure_Me.cmd` again and verify that the expected Java installation and optional components are detected.

For additional help, I don't know. Contact [**GaiusCassius**](https://discord.com/users/301544769811775488) on Discord I guess.

## Disclaimer

This is an unofficial community package. It is not affiliated with Starsector, Fractal Softworks, Adoptium, or the authors of optional third-party mods.

The included Mikohime files are redistributed with permission from the mod author. Credit remains with the original modders and maintainers.
