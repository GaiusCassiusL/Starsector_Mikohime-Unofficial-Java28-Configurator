package com.apple.eio;

import java.io.IOException;

/**
 * Compile-time-only stub for Apple's {@code com.apple.eio.FileManager} class,
 * which only exists in Apple's own historical JDK builds (the class is
 * absent from mainline OpenJDK/Temurin on Windows and Linux).
 *
 * {@code org.lwjgl.MacOSXSysImplementation} references
 * {@link #openURL(String)} to open a browser window on macOS. That branch
 * never executes on Windows or Linux, but the class must still resolve at
 * compile time. This stub reproduces only the one method signature actually
 * referenced by the recovered LWJGL source (mirroring the well-known
 * "AppleJavaExtensions" compile-time stub jar Apple historically published
 * for exactly this purpose) and is never packaged into the produced lwjgl.jar
 * (it is a {@code compileOnly} dependency only).
 */
public final class FileManager {
    private FileManager() {
    }

    public static void openURL(String url) throws IOException {
        throw new UnsupportedOperationException("com.apple.eio.FileManager is a compile-time-only stub");
    }
}
