package org.lwjgl.opengles;

import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.PixelFormatLWJGL;

/**
 * Compile-time-only stub: the original LWJGL 2 opengles PixelFormat class was not
 * decompiled from the binary. This stub exists solely so the opengl classes
 * (DrawableGLES, LinuxDisplay, WindowsDisplay) that reference it can compile.
 */
public class PixelFormat implements PixelFormatLWJGL {
    public IntBuffer getAttribBuffer(EGLDisplay display, int surfaceType, int[] extraAttribs) throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public EGLConfig getBestMatch(EGLConfig[] configs) throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public void setSurfaceAttribs(EGLSurface surface) throws LWJGLException {
        throw new UnsupportedOperationException();
    }
}
