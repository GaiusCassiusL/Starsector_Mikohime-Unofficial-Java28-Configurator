package org.lwjgl.opengles;

import org.lwjgl.LWJGLException;

/**
 * Compile-time-only stub: the original LWJGL 2 opengles GLContext class was not
 * decompiled from the binary. This stub exists solely so the opengl classes
 * (ContextGLES, DrawableGLES, LinuxDisplayPeerInfo, WindowsDisplayPeerInfo,
 * LinuxDisplay) that reference it can compile.
 */
public class GLContext {
    public static void loadOpenGLLibrary() throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public static void unloadOpenGLLibrary() {
        throw new UnsupportedOperationException();
    }

    public static void useContext(Object context) throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public static GLESCapabilities getCapabilities() {
        throw new UnsupportedOperationException();
    }
}
