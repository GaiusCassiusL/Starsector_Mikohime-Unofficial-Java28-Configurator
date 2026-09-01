package org.apache.log4j.helpers;

import java.io.File;

public abstract class FileWatchdog extends Thread {
   public static final long DEFAULT_DELAY = 60000L;
   protected String filename;
   protected long delay = 60000L;
   File file;
   long lastModified;
   boolean warnedAlready;
   boolean interrupted;

   protected FileWatchdog(final String fileName) {
      super("FileWatchdog");
      this.filename = fileName;
      this.file = new File(fileName);
      this.setDaemon(true);
      this.checkAndConfigure();
   }

   protected void checkAndConfigure() {
      boolean fileExists;
      try {
         fileExists = this.file.exists();
      } catch (SecurityException e) {
         LogLog.warn("Was not allowed to read check file existance, file:[" + this.filename + "].");
         this.interrupted = true;
         return;
      }

      if (fileExists) {
         long fileLastMod = this.file.lastModified();
         if (fileLastMod > this.lastModified) {
            this.lastModified = fileLastMod;
            this.doOnChange();
            this.warnedAlready = false;
         }
      } else if (!this.warnedAlready) {
         LogLog.debug("[" + this.filename + "] does not exist.");
         this.warnedAlready = true;
      }
   }

   protected abstract void doOnChange();

   @Override
   public void run() {
      while (!this.interrupted) {
         try {
            Thread.sleep(this.delay);
         } catch (InterruptedException var2) {
         }

         this.checkAndConfigure();
      }
   }

   public void setDelay(final long delayMillis) {
      this.delay = delayMillis;
   }
}
