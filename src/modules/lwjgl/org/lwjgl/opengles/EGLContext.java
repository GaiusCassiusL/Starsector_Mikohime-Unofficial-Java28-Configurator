package org.lwjgl.opengles;

import org.lwjgl.LWJGLException;

/**
 * Compile-time-only stub: the original LWJGL 2 opengles EGLContext class was not
 * decompiled from the binary. This stub exists solely so the opengl classes
 * (ContextGLES, DrawableGLES) that reference it can compile.
 */
public class EGLContext {
    public void setDisplay(EGLDisplay display) {
        throw new UnsupportedOperationException();
    }

    public void makeCurrent(EGLSurface surface) throws LWJGLException, PowerManagementEventException {
        throw new UnsupportedOperationException();
    }

    public void destroy() throws LWJGLException {
        throw new UnsupportedOperationException();
    }
}
