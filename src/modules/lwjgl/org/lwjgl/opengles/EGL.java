package org.lwjgl.opengles;

import org.lwjgl.LWJGLException;

/**
 * Compile-time-only stub: the original LWJGL 2 opengles EGL class was not
 * decompiled from the binary. This stub exists solely so the opengl classes
 * (ContextGLES, DrawableGLES) that reference it can compile.
 */
public class EGL {
    public static EGLDisplay eglGetDisplay(int display_id) throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public static void eglReleaseCurrent(EGLDisplay display) throws LWJGLException, PowerManagementEventException {
        throw new UnsupportedOperationException();
    }

    public static boolean eglIsCurrentContext(EGLContext context) throws LWJGLException {
        throw new UnsupportedOperationException();
    }
}
