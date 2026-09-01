# Changelog

## [v0.6] - 2026-09-01

### Added

- Added StarsectorPrepatcher version detection using each installation's `mod_info.json`.
- Added a minimum supported StarsectorPrepatcher version of `0.18.4`.
- Added version numbers beside compatible StarsectorPrepatcher installations in the selection menu.
- Added informational detection for VRAM Optimizer.
- Added a recommendation and GitHub download link when VRAM Optimizer is not installed.
- Added VRAM Optimizer to the missing-component download menu.

### Changed

- Simplified the detected-environment display so compatible StarsectorPrepatcher installations are shown as **Installed**.
- Changed StarsectorPrepatcher detection to exclude versions older than `0.18.4`.

### Fixed

- Prevented incompatible StarsectorPrepatcher versions from being offered for activation.
- Prevented installations with missing or unreadable StarsectorPrepatcher version metadata from being treated as compatible.

## [v0.5] - 2026-08-31

### Added

- Added all required Mikohime files directly to the release package with
  permission from the original mod author. The original Mikohime package no
  longer needs to be downloaded separately.
- Added a download link and direct download-page option for FR Resource Cache.

### Changed

- Updated installation instructions for the new self-contained release package.

### Fixed

- Fixed Back navigation skipping the StarsectorPrepatcher selection menu.

## [v0.4] - 2026-08-30

### Added

- Added direct support for the latest StarsectorPrepatcher release on Java 17,
  Java 27, and Java 28.
- Added automatic detection of folders matching `StarsectorPrepatcher*`.
- Added a choice to enable or disable StarsectorPrepatcher when it is detected.
- Added automatic detection and optional activation of Fast Rendering and FR
  Resource Cache.
- Added GitHub release links when Fast Rendering or StarsectorPrepatcher is not
  detected.
- Added an option to open missing component download pages directly from the
  configurator.
- Added Java 17, Java 27, and Java 28 installation detection.
- Added support for selecting a custom Java installation folder.
- Added automatic physical memory, physical CPU core, and available logical
  processor detection.
- Added memory recommendations, custom memory allocation, and advanced memory
  options.
- Added a warning when the selected Java heap exceeds 75% of detected physical
  memory.
- Added low-core CPU tuning based on the logical processors currently available
  to Java.
- Added compatibility tuning for older Intel processors that do not support
  newer AVX instructions.
- Added Large Pages privilege detection and a warning that Java commits the full
  selected heap at startup.
- Added Full, Reduced, and Minimal logging modes.
- Added a Minimal logging mode that comments out optional JVM diagnostic and
  logger arguments.
- Added colored status messages for detected components, warnings, and errors.
- Added a review screen showing the selected configuration before files are
  created.
- Added validation and transactional installation of generated configuration
  files.
- Added a single-instance lock to prevent multiple configurators from running
  simultaneously.
- Added unique pending and backup filenames to prevent temporary-file
  collisions.

### Changed

- Removed the obsolete StarsectorPrepatcher Java 27/28 compatibility JAR
  integration.
- Changed StarsectorPrepatcher integration to load only the official
  `StarsectorPrepatcherAgent.jar`.
- Improved detection of exact and version-suffixed StarsectorPrepatcher folders.
- Improved handling of multiple StarsectorPrepatcher installations. The
  configurator lists each valid `StarsectorPrepatcher*` folder and lets the user
  enable a specific installation or disable Prepatcher from the same menu.
- Standardized menu input so selections use consistent single-key controls.
- Redesigned the configurator into a guided seven-step setup process.
- Improved the header, spacing, alignment, status display, prompts, warnings,
  and navigation.
- Reduced the prominence of unnecessary 64 GB and 128 GB memory options.
- Moved extreme memory allocations behind a dedicated warning screen.
- Changed low-core recommendations to use available logical processors instead
  of physical cores.
- Improved Java version detection when environment messages or warnings appear
  before the version line.
- Improved handling of custom input and detected folders containing CMD
  metacharacters.
- Changed `-XX:MaxGCPauseMillis=100` to a commented JVM option so users may
  enable or edit it manually.
- Added `-XX:+DisableExplicitGC` to the Java 28 configuration.
- Preserved default JVM object alignment by removing the explicit 8-byte
  alignment argument.
- Improved generated-file validation and rollback behavior to protect existing
  configurations.

### Removed

- Removed all references to `Java28PrepatcherCompat`.
- Removed `prepatcher-java-base-asm.jar`.
- Removed `prepatcher-java28-frame-repair-agent.jar`.
- Removed the obsolete `-XX:CompileCommand=exclude,*.readResolve` JVM argument.
- Removed explicit object-alignment configuration.

### Fixed

- Fixed generated launcher corruption when `mikohime\DefaultPath` does not end
  with a newline.
- Fixed Java version detection failing when `JAVA_TOOL_OPTIONS` or another
  message appears first.
- Fixed unsafe delayed expansion that could corrupt user-entered or detected
  paths.
- Fixed misleading physical CPU core reporting when detection fails.
- Fixed memory warnings only appearing after the selected heap exceeded all
  physical RAM.
- Fixed potential collisions between pending and backup files.
- Fixed concurrent configurator instances being able to overwrite each other's
  temporary files.
- Fixed Prepatcher agent paths failing when the selected folder name contains
  spaces.
- Fixed detected but disabled Prepatcher installations being reported as not
  installed.

## [v0.3] - 2026-08-28

### Added

- Added an option to hide informational terminal output while continuing to
  display warnings and errors.
- Added an option for future integration with an unofficial Starsector
  Prepatcher compatibility patch. This integration does not modify any of
  cyrrp's files; the original files must still be installed according to the
  author's instructions.
- Added an option for future integration with the unofficial FR Resource Cache
  bolt-on mod. This integration does not modify any of Genir's files; the
  original files must still be installed according to the author's instructions.

## [v0.2] - 2026-08-26

### Fixed

- Fixed a crash-to-desktop issue that could occur when loading saved games.

## [v0.1] - 2026-08-24

### Added

- Initial release.

[v0.6]: https://github.com/GaiusCassiusL/Starsector_Mikohime-Unofficial-Java28-Configurator/releases/tag/v0.6
[v0.5]: https://github.com/GaiusCassiusL/Starsector_Mikohime-Unofficial-Java28-Configurator/releases/tag/v0.5
[v0.4]: https://github.com/GaiusCassiusL/Starsector_Mikohime-Unofficial-Java28-Configurator/releases/tag/v0.4
[v0.3]: https://github.com/GaiusCassiusL/Starsector_Mikohime-Unofficial-Java28-Configurator/releases/tag/v0.3
[v0.2]: https://github.com/GaiusCassiusL/Starsector_Mikohime-Unofficial-Java28-Configurator/releases/tag/v0.2
[v0.1]: https://github.com/GaiusCassiusL/Starsector_Mikohime-Unofficial-Java28-Configurator/tree/v0.1
