package org.lwjgl.opengles;

import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;

/**
 * Compile-time-only stub: the original LWJGL 2 opengles EGLDisplay class was not
 * decompiled from the binary. This stub exists solely so the opengl classes
 * (DrawableGLES, ContextGLES) that reference it can compile.
 */
public class EGLDisplay {
    public void terminate() throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public EGLConfig[] chooseConfig(IntBuffer attrib_list, EGLConfig[] configs, IntBuffer num_config) throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public EGLSurface createWindowSurface(EGLConfig config, long window, IntBuffer attrib_list) throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public void setSwapInterval(int interval) throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    public EGLContext createContext(EGLConfig config, EGLContext shared, IntBuffer attrib_list) throws LWJGLException {
        throw new UnsupportedOperationException();
    }
}
