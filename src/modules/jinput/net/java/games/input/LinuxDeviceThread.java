package net.java.games.input;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

final class LinuxDeviceThread extends Thread {
   private final Deque<LinuxDeviceTask> tasks = new ArrayDeque<>();

   public LinuxDeviceThread() {
      this.setDaemon(true);
      this.start();
   }

   public final synchronized void run() {
      while (true) {
         while (this.tasks.isEmpty()) {
            try {
               this.wait();
            } catch (InterruptedException e) {
            }
         }

         LinuxDeviceTask task = this.tasks.removeFirst();
         task.doExecute();
         synchronized (task) {
            task.notify();
         }
      }
   }

   public final Object execute(LinuxDeviceTask task) throws IOException {
      synchronized (this) {
         this.tasks.addLast(task);
         this.notify();
      }

      synchronized (task) {
         while (task.getState() == 1) {
            try {
               task.wait();
            } catch (InterruptedException e) {
            }
         }
      }

      switch (task.getState()) {
         case 2:
            return task.getResult();
         case 3:
            throw task.getException();
         default:
            throw new RuntimeException("Invalid task state: " + task.getState());
      }
   }
}
