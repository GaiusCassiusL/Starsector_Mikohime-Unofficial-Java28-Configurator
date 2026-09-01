package org.lwjgl.opengles;

import org.lwjgl.LWJGLException;

/**
 * Compile-time-only stub: the original LWJGL 2 opengles EGLSurface class was not
 * decompiled from the binary. This stub exists solely so the opengl classes
 * (DrawableGLES, ContextGLES) that reference it can compile.
 */
public class EGLSurface {
    public void destroy() throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public void swapBuffers() throws LWJGLException, PowerManagementEventException {
        throw new UnsupportedOperationException();
    }
}
