# Mikohime-Unofficial-Java28-Configurator
Make Java 28 work with Starsector (probably)

Note: If you have crashes with the game that look like Fast Rendering and you're using J28, hit me up first on discord (Gaius Cassius) before poking Genir. 


**Mikohime Unofficial Configurator - Java 28-12ea**

Replaces the original Configure_Me.cmd file to generate a Miko_Rouge.bat and Miko_Simple.txt file that will work with Java 28-11ea or Java 28-12ea.

Requires Mikoe26_261_FRChange from Yue on the Unofficial Starsector Discord:
https://cdn.discordapp.com/attachments/1466341118261072028/1534729174185672825/Miko26_261_FRChange.zip?ex=6a8e3bed&is=6a8cea6d&hm=6312e17e80e4d07265c377beaea50b1bf3b7bc736916e09fd7e61c2e4cd6981d&

Java 27-11ea: https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27%2B22-ea-beta/OpenJDK-jdk_x64_windows_hotspot_27_22-ea.zip

Java 28-11ea: https://github.com/adoptium/temurin28-binaries/releases/download/jdk-28%2B11-ea-beta/OpenJDK-jdk_x64_windows_hotspot_28_11-ea.zip

Java 28-12ea: https://github.com/adoptium/temurin28-binaries/releases/download/jdk-28%2B12-ea-beta/OpenJDK-jdk_x64_windows_hotspot_28_12-ea.zip

**Installation**
-------------
1) Install the original Miko package in the Starsector directory. (.\Starsector)
2) Replace Configure_Me.cmd from original Miko package with Configure_Me.cmd located in the Unofficial Configurator.
3) Keep the bundled .\jre folder. Extract Java 27, 28, or both into additional folders beside starsector.exe. (Example: .\Starsector\jdk-27+22 or .\Starsector\jdk-28+12)
4) Run Configure_Me.cmd and select a detected Java installation along with the normal configurations from before (VRAM, Fast Rendering, etc) - Run-As Admin if you have your installation in a Windows Program Files folder.
5) Launch using the generated Miko_Rouge.cmd.
