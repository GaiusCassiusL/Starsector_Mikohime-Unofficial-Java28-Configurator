# Third-party notices

This file identifies third-party components and assets included with or
referenced by the Mikohime distribution. These materials are **not** licensed
under the project license in `LICENSE`; their own license terms and notices
apply. The component JARs remain unmodified except where the notes below
identify project overlays or rebuilds.

Some component JARs contain their upstream license and notice files; the table
notes where. For components without embedded license files, the upstream
project/license links are provided below. Do not remove these references or
upstream metadata when redistributing. Corresponding JOrbis source is
maintained under `src/modules/jorbis/`; keep source for the matching release
available when redistributing its LGPL-licensed binary.

## Java components

| Distributed artifact | Version / origin | License | Notes |
| --- | --- | --- | --- |
| `commons-compiler-3.0.12.jar`, `commons-compiler-jdk-3.0.12.jar`, `janino-3.0.12.jar` | Janino 3.0.12 | BSD-3-Clause | Resolved from Maven Central. [Upstream license](https://github.com/janino-compiler/janino/blob/v3.0.12/LICENSE). |
| `disruptor-4.0.0.jar` | LMAX Disruptor 4.0.0 | Apache-2.0 | Resolved from Maven Central. [Upstream project](https://github.com/LMAX-Exchange/disruptor). |
| `jaxb-api-2.4.0-b180830.0359.jar` | JAXB API 2.4.0 | CDDL-1.1 OR GPL-2.0-with-classpath-exception | Resolved from Maven Central. The JAR includes its upstream license text. |
| `log4j-api-3.0.0-alpha1.jar`, `log4j-plugins-3.0.0-alpha1.jar`, `log4j-1.2-api-3.0.0-alpha1.jar`, `log4j-core-3.0.0-alpha1.jar` | Apache Log4j 3.0.0-alpha1 | Apache-2.0 | Resolved upstream JARs retain their notices. The distribution rebuilds/overlays the custom plugin descriptors in `log4j-1.2-api` and `log4j-core`; the upstream Log4j license continues to apply to the underlying components. |
| `txw2-3.0.2.jar` | TXW2 3.0.2 | BSD-3-Clause | Resolved from Maven Central. The JAR includes upstream `META-INF/LICENSE.md` and `META-INF/NOTICE.md`. |
| `jcraft-jorbis-0.0.17.jar` | JOrbis 0.0.17 | LGPL-2.1-or-later | Rebuilt from the corresponding source under `src/modules/jorbis/`; the build omits `module-info.class` for the classpath distribution. [Upstream project](https://github.com/ymnk/jorbis); [LGPL-2.1 text](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html). |
| `jinput.jar` | Legacy JInput | BSD-2-Clause | Rebuilt from recovered source. The exact version of the original Windows native binaries is not encoded in those files. [Upstream project and license](https://github.com/jinput/jinput). |
| `lwjgl.jar`, `lwjgl_util.jar` | Legacy LWJGL 2 | BSD-3-Clause | Rebuilt from recovered source. [Upstream project](https://github.com/LWJGL/lwjgl). |
| `xstream-1.4.21_miko.jar` | XStream 1.4.21 | BSD-3-Clause | Rebuilt with project overlays in seven XStream/Types class files. The XStream license applies to the upstream classes. [Upstream license](https://x-stream.github.io/license.html). |

## Native libraries

| Files | Origin / license | Notes |
| --- | --- | --- |
| Linux `liblwjgl64.so`, `libopenal64.so` | `org.jmonkeyengine:lwjgl-platform:2.9.5:natives-linux`; LWJGL is BSD-3-Clause and OpenAL Soft is LGPL-2.1-or-later | Extracted by the Linux distribution build from the declared Maven artifact. |
| Linux `libjinput-linux64.so` | `net.java.jinput:jinput:2.0.10:natives-all`; BSD-2-Clause | Extracted by the Linux distribution build from the declared Maven artifact. |
| Windows `lwjgl.dll`, `lwjgl64.dll` | LWJGL 2 native libraries; BSD-3-Clause | The exact binary version/provenance is not encoded in the files. |
| Windows `jinput-dx8*.dll`, `jinput-raw*.dll` | JInput native libraries; BSD-2-Clause | The exact binary version/provenance is not encoded in the files. |
| Windows `OpenAL32.dll`, `OpenAL64.dll` | Identified by embedded strings as OpenAL Soft 1.15.1; LGPL-2.1-or-later | The exact upstream binary package/source correspondence has not been confirmed. Verify and preserve the applicable source and license materials when redistributing these binaries. [LGPL-2.1 text](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html). |

## Artwork and other separately licensed material

- Original Mikohime artwork and resources by Yue (Himemiko) are included with
  the author's permission and are covered by the project terms in `LICENSE`.
- The Australian Museum cane-toad image embedded by remote URL in the
  repository README is not included as an image file in the release archives.
  It remains subject to the [Australian Museum's Educational & Non-Commercial
  Terms](https://australian.museum/copyright/) and is not covered by the
  project license.
- Optional mods, OpenAL Soft add-ons, and Adoptium Java runtimes downloaded by
  the configurator are not included in this distribution. Their respective
  authors' licenses and download terms apply.
