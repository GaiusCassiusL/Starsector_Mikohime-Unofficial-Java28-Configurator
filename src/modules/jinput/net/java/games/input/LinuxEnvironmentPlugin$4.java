package net.java.games.input;

import java.security.PrivilegedAction;

class LinuxEnvironmentPlugin$4 implements PrivilegedAction {
   // decompiler artifact: missing synthetic outer-class reference field declaration
   final LinuxEnvironmentPlugin this$0;

   LinuxEnvironmentPlugin$4(LinuxEnvironmentPlugin var1) {
      this.this$0 = var1;
   }

   public final Object run() {
      // decompiler artifact: Vineflower emits outer.new Inner(null) for no-arg inner class; correct form has no args
      Runtime.getRuntime().addShutdownHook(this.this$0.new ShutdownHook());
      return null;
   }
}
