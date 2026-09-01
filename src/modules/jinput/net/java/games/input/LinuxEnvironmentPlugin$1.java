package net.java.games.input;

import java.io.File;
import java.security.PrivilegedAction;

final class LinuxEnvironmentPlugin$1 implements PrivilegedAction {
   // decompiler artifact: missing synthetic captured-variable field declaration
   private final String val$lib_name;

   LinuxEnvironmentPlugin$1(String var1) {
      this.val$lib_name = var1;
   }

   public final Object run() {
      String lib_path = System.getProperty("net.java.games.input.librarypath");

      try {
         if (lib_path != null) {
            System.load(lib_path + File.separator + System.mapLibraryName(this.val$lib_name));
         } else {
            System.loadLibrary(this.val$lib_name);
         }
      } catch (UnsatisfiedLinkError e) {
         ControllerEnvironment.logln("Failed to load library: " + e.getMessage());
         e.printStackTrace();
         LinuxEnvironmentPlugin.access$002(false);
      }

      return null;
   }
}
