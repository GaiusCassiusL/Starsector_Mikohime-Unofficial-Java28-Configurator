package org.lwjgl.input;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Controller.Type;
import org.lwjgl.LWJGLException;

public class Controllers {
   private static final ArrayList<JInputController> controllers = new ArrayList<>();
   private static int controllerCount;
   private static final Deque<ControllerEvent> events = new ArrayDeque<>();
   private static final Object eventLock = new Object();
   private static volatile ControllerEvent event;
   private static boolean created;

   public static void create() throws LWJGLException {
      if (!created) {
         try {
            ControllerEnvironment env = ControllerEnvironment.getDefaultEnvironment();
            net.java.games.input.Controller[] found = env.getControllers();
            ArrayList<net.java.games.input.Controller> lollers = new ArrayList<>();

            for (net.java.games.input.Controller c : found) {
               if (!c.getType().equals(Type.KEYBOARD) && !c.getType().equals(Type.MOUSE)) {
                  lollers.add(c);
               }
            }

            for (net.java.games.input.Controller c : lollers) {
               createController(c);
            }

            created = true;
         } catch (Throwable e) {
            throw new LWJGLException("Failed to initialise controllers", e);
         }
      }
   }

   private static void createController(net.java.games.input.Controller c) {
      net.java.games.input.Controller[] subControllers = c.getControllers();
      if (subControllers.length == 0) {
         JInputController controller = new JInputController(controllerCount, c);
         controllers.add(controller);
         controllerCount++;
      } else {
         for (net.java.games.input.Controller sub : subControllers) {
            createController(sub);
         }
      }
   }

   public static Controller getController(int index) {
      return controllers.get(index);
   }

   public static int getControllerCount() {
      return controllers.size();
   }

   public static void poll() {
      for (int i = 0; i < controllers.size(); i++) {
         getController(i).poll();
      }
   }

   public static void clearEvents() {
      synchronized (eventLock) {
         events.clear();
      }
   }

   public static boolean next() {
      synchronized (eventLock) {
         event = events.pollFirst();
         return event != null;
      }
   }

   public static boolean isCreated() {
      return created;
   }

   public static void destroy() {
   }

   public static Controller getEventSource() {
      return event.getSource();
   }

   public static int getEventControlIndex() {
      return event.getControlIndex();
   }

   public static boolean isEventButton() {
      return event.isButton();
   }

   public static boolean isEventAxis() {
      return event.isAxis();
   }

   public static boolean isEventXAxis() {
      return event.isXAxis();
   }

   public static boolean isEventYAxis() {
      return event.isYAxis();
   }

   public static boolean isEventPovX() {
      return event.isPovX();
   }

   public static boolean isEventPovY() {
      return event.isPovY();
   }

   public static long getEventNanoseconds() {
      return event.getTimeStamp();
   }

   public static boolean getEventButtonState() {
      return event.getButtonState();
   }

   public static float getEventXAxisValue() {
      return event.getXAxisValue();
   }

   public static float getEventYAxisValue() {
      return event.getYAxisValue();
   }

   static void addEvent(ControllerEvent event) {
      if (event != null) {
         synchronized (eventLock) {
            events.addLast(event);
         }
      }
   }
}
