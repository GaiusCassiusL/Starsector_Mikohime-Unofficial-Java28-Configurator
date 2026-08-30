# Mikohime Unofficial Java 28 Configurator

An unofficial replacement `Configure_Me.cmd` that adds Java 27 and Java 28 early-access support to Starsector.

The configurator generates:

- `Miko_Rouge.bat`
- `Miko_Simple.txt`

> [!IMPORTANT]
> Currently supports Java 28 builds `28+11-ea` and `28+12-ea`.

## Requirements

Install **Mikoe26_261_FRChange** by Yue from the Unofficial Starsector Discord:

[Download Mikoe26_261_FRChange](https://cdn.discordapp.com/attachments/1466341118261072028/1534729174185672825/Miko26_261_FRChange.zip?ex=6a8e3bed&is=6a8cea6d&hm=6312e17e80e4d07265c377beaea50b1bf3b7bc736916e09fd7e61c2e4cd6981d&)

## Java Downloads

| Version | Download |
| --- | --- |
| Java 27 (`27+22-ea`) | [Download from Adoptium](https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27%2B22-ea-beta/OpenJDK-jdk_x64_windows_hotspot_27_22-ea.zip) |
| Java 28 (`28+11-ea`) | [Download from Adoptium](https://github.com/adoptium/temurin28-binaries/releases/download/jdk-28%2B11-ea-beta/OpenJDK-jdk_x64_windows_hotspot_28_11-ea.zip) |
| Java 28 (`28+12-ea`) | [Download from Adoptium](https://github.com/adoptium/temurin28-binaries/releases/download/jdk-28%2B12-ea-beta/OpenJDK-jdk_x64_windows_hotspot_28_12-ea.zip) |
| Java 28 (`28+13-ea`) | [Download from Adoptium](https://github.com/adoptium/temurin28-binaries/releases/download/jdk-28%2B13-ea-beta/OpenJDK-jdk_x64_windows_hotspot_28_13-ea.zip) |

## Installation

1. Install the original Miko package in your Starsector directory:

   ```text
   .\Starsector
   ```

2. Replace the package's original `Configure_Me.cmd` with the version from this repository.

3. Keep Starsector's bundled `jre` folder.

4. Extract Java 27, Java 28, or both into separate folders beside `starsector.exe`.

   Example:

   ```text
   .\Starsector\jdk-27+22
   .\Starsector\jdk-28+12
   ```

5. Run `Configure_Me.cmd`.

   > If Starsector is installed under `Program Files`, you may need to run the configurator as an administrator.

6. Select a detected Java installation and configure the usual options, such as VRAM and Fast Rendering.

7. Launch Starsector using the generated `Miko_Rouge.bat`.

## Troubleshooting

I don't know. Do it again or contact [**GaiusCassius**](https://discord.com/users/301544769811775488) on Discord.

## Disclaimer

This is an unofficial configurator. It is not affiliated with the original Miko package authors, Starsector, or Adoptium.
