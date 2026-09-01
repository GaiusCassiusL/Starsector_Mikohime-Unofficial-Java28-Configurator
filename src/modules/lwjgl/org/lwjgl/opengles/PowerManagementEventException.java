package org.lwjgl.opengles;

import org.lwjgl.LWJGLException;

/**
 * Compile-time-only stub: the original LWJGL 2 opengles PowerManagementEventException
 * class was not decompiled from the binary. This stub exists solely so the opengl
 * classes (ContextGLES, DrawableGLES) that reference it can compile.
 * Extends LWJGLException (as the original does) so existing catch(LWJGLException) blocks
 * automatically cover it and callers don't need separate exception-handling.
 */
public class PowerManagementEventException extends LWJGLException {
    public PowerManagementEventException() {
        super();
    }

    public PowerManagementEventException(String message) {
        super(message);
    }
}
