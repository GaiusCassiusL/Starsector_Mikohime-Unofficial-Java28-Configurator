package org.lwjgl.opengl;

import java.awt.Canvas;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;

final class LinuxCanvasImplementation implements AWTCanvasImplementation {
   static int getScreenFromDevice(final GraphicsDevice device) throws LWJGLException {
      try {
         Method getScreen_method = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
            public Method run() throws Exception {
               return device.getClass().getMethod("getScreen");
            }
         });
         Integer screen = (Integer)getScreen_method.invoke(device);
         return screen;
      } catch (Exception e) {
         throw new LWJGLException(e);
      }
   }

   private static int getVisualIDFromConfiguration(final GraphicsConfiguration configuration) throws LWJGLException {
      try {
         Method getVisual_method = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
            public Method run() throws Exception {
               return configuration.getClass().getMethod("getVisual");
            }
         });
         Integer visual = (Integer)getVisual_method.invoke(configuration);
         return visual;
      } catch (Exception e) {
         throw new LWJGLException(e);
      }
   }

   @Override
   public PeerInfo createPeerInfo(Canvas component, PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
      return new LinuxAWTGLCanvasPeerInfo(component);
   }

   @Override
   public GraphicsConfiguration findConfiguration(GraphicsDevice device, PixelFormat pixel_format) throws LWJGLException {
      try {
         int screen = getScreenFromDevice(device);
         int visual_id_matching_format = findVisualIDFromFormat(screen, pixel_format);
         GraphicsConfiguration[] configurations = device.getConfigurations();

         for (GraphicsConfiguration configuration : configurations) {
            int visual_id = getVisualIDFromConfiguration(configuration);
            if (visual_id == visual_id_matching_format) {
               return configuration;
            }
         }
      } catch (LWJGLException e) {
         LWJGLUtil.log("Got exception while trying to determine configuration: " + e);
      }

      return null;
   }

   private static int findVisualIDFromFormat(int screen, PixelFormat pixel_format) throws LWJGLException {
      try {
         LinuxDisplay.lockAWT();

         try {
            GLContext.loadOpenGLLibrary();

            try {
               LinuxDisplay.incDisplay();
               return nFindVisualIDFromFormat(LinuxDisplay.getDisplay(), screen, pixel_format);
            } finally {
               LinuxDisplay.decDisplay();
            }
         } finally {
            GLContext.unloadOpenGLLibrary();
         }
      } finally {
         LinuxDisplay.unlockAWT();
      }
   }

   private static native int nFindVisualIDFromFormat(long var0, int var2, PixelFormat var3) throws LWJGLException;
}
