package net.java.games.input;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class ControllerEnvironment {
   // decompiler artifact: synthetic field for assert statements
   static final boolean $assertionsDisabled = !ControllerEnvironment.class.desiredAssertionStatus();
   private static ControllerEnvironment defaultEnvironment = new DefaultControllerEnvironment();
   protected final ArrayList controllerListeners = new ArrayList();

   static void logln(String msg) {
      log(msg + "\n");
   }

   static void log(String msg) {
      System.out.print(msg);
   }

   protected ControllerEnvironment() {
   }

   public abstract Controller[] getControllers();

   public void addControllerListener(ControllerListener l) {
      if (!$assertionsDisabled && l == null) {
         throw new AssertionError();
      }

      this.controllerListeners.add(l);
   }

   public abstract boolean isSupported();

   public void removeControllerListener(ControllerListener l) {
      if (!$assertionsDisabled && l == null) {
         throw new AssertionError();
      }

      this.controllerListeners.remove(l);
   }

   protected void fireControllerAdded(Controller c) {
      ControllerEvent ev = new ControllerEvent(c);
      Iterator it = this.controllerListeners.iterator();

      while (it.hasNext()) {
         ((ControllerListener)it.next()).controllerAdded(ev);
      }
   }

   protected void fireControllerRemoved(Controller c) {
      ControllerEvent ev = new ControllerEvent(c);
      Iterator it = this.controllerListeners.iterator();

      while (it.hasNext()) {
         ((ControllerListener)it.next()).controllerRemoved(ev);
      }
   }

   public static ControllerEnvironment getDefaultEnvironment() {
      return defaultEnvironment;
   }
}
