package net.java.games.input;

import java.security.PrivilegedAction;

final class DirectInputEnvironmentPlugin$2 implements PrivilegedAction {
   // decompiler artifact: missing synthetic captured-variable field declaration
   private final String val$property;

   DirectInputEnvironmentPlugin$2(String var1) {
      this.val$property = var1;
   }

   public Object run() {
      return System.getProperty(this.val$property);
   }
}
