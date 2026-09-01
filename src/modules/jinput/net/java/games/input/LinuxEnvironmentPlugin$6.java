package net.java.games.input;

import java.io.File;
import java.security.PrivilegedAction;

final class LinuxEnvironmentPlugin$6 implements PrivilegedAction {
   // decompiler artifact: missing synthetic captured-variable field declaration
   private final File val$file;

   LinuxEnvironmentPlugin$6(File var1) {
      this.val$file = var1;
   }

   public Object run() {
      return this.val$file.getAbsolutePath();
   }
}
