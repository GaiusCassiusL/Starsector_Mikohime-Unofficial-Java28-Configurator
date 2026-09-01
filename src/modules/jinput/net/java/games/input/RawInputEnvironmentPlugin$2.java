package net.java.games.input;

import java.security.PrivilegedAction;

final class RawInputEnvironmentPlugin$2 implements PrivilegedAction {
   // decompiler artifact: missing synthetic captured-variable field declaration
   private final String val$property;

   RawInputEnvironmentPlugin$2(String var1) {
      this.val$property = var1;
   }

   public Object run() {
      return System.getProperty(this.val$property);
   }
}
