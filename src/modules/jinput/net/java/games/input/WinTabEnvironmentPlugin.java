package net.java.games.input;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import net.java.games.util.plugins.Plugin;

public class WinTabEnvironmentPlugin extends ControllerEnvironment implements Plugin {
   private static boolean supported = false;
   private final Controller[] controllers;
   private final List active_devices = new ArrayList();
   private final WinTabContext winTabContext;

   static void loadLibrary(String lib_name) {
      AccessController.doPrivileged(new WinTabEnvironmentPlugin$1(lib_name));
   }

   static String getPrivilegedProperty(String property) {
      // decompiler artifact: doPrivileged returns Object; cast to expected return type
      return (String) AccessController.doPrivileged(new WinTabEnvironmentPlugin$2(property));
   }

   static String getPrivilegedProperty(String property, String default_value) {
      // decompiler artifact: doPrivileged returns Object; cast to expected return type
      return (String) AccessController.doPrivileged(new WinTabEnvironmentPlugin$3(property, default_value));
   }

   public WinTabEnvironmentPlugin() {
      if (this.isSupported()) {
         DummyWindow window = null;
         WinTabContext winTabContext = null;
         Controller[] controllers = new Controller[0];

         try {
            window = new DummyWindow();
            winTabContext = new WinTabContext(window);

            try {
               winTabContext.open();
               controllers = winTabContext.getControllers();
            } catch (Exception e) {
               window.destroy();
               throw e;
            }
         } catch (Exception e) {
            logln("Failed to enumerate devices: " + e.getMessage());
            e.printStackTrace();
         }

         this.controllers = controllers;
         this.winTabContext = winTabContext;
         AccessController.doPrivileged(new WinTabEnvironmentPlugin$4(this));
      } else {
         this.winTabContext = null;
         this.controllers = new Controller[0];
      }
   }

   public boolean isSupported() {
      return supported;
   }

   public Controller[] getControllers() {
      return this.controllers;
   }

   static {
      String osName = getPrivilegedProperty("os.name", "").trim();
      if (osName.startsWith("Windows")) {
         supported = true;
         loadLibrary("jinput-wintab");
      }
   }

   // decompiler artifact: 'private' widened to package so separate-compilation-unit $4.java can create instances
   final class ShutdownHook extends Thread {
      // decompiler artifact: 'private' widened to package so $4.java can call the constructor
      ShutdownHook() {
      }

      public final void run() {
         int i = 0;

         while (i < WinTabEnvironmentPlugin.this.active_devices.size()) {
            i++;
         }

         WinTabEnvironmentPlugin.this.winTabContext.close();
      }
   }

   // decompiler artifact: synthetic accessor for private static field 'supported', called from inner class
   static boolean access$002(boolean val) { return supported = val; }
}
