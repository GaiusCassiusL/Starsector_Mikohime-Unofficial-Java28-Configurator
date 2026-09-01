package net.java.games.input;

import java.security.PrivilegedAction;

class WinTabEnvironmentPlugin$4 implements PrivilegedAction {
   // decompiler artifact: missing synthetic outer-class reference field declaration
   final WinTabEnvironmentPlugin this$0;

   WinTabEnvironmentPlugin$4(WinTabEnvironmentPlugin var1) {
      this.this$0 = var1;
   }

   public final Object run() {
      // decompiler artifact: Vineflower emits outer.new Inner(null) for no-arg inner class; correct form has no args
      Runtime.getRuntime().addShutdownHook(this.this$0.new ShutdownHook());
      return null;
   }
}
