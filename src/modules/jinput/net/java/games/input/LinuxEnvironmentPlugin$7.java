package net.java.games.input;

import java.io.File;
import java.io.FilenameFilter;
import java.security.PrivilegedAction;

final class LinuxEnvironmentPlugin$7 implements PrivilegedAction {
   // decompiler artifact: missing synthetic captured-variable field declarations
   private final File val$dir;
   private final FilenameFilter val$filter;

   LinuxEnvironmentPlugin$7(File var1, FilenameFilter var2) {
      this.val$dir = var1;
      this.val$filter = var2;
   }

   public Object run() {
      return this.val$dir.listFiles(this.val$filter);
   }
}
