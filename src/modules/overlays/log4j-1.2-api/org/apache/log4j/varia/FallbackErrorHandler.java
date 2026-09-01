package org.apache.log4j.varia;

import java.io.InterruptedIOException;
import java.util.Vector;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.LoggingEvent;

public class FallbackErrorHandler implements ErrorHandler {
   Appender backup;
   Appender primary;
   Vector loggers;

   public void activateOptions() {
   }

   @Override
   public void error(final String message) {
   }

   @Override
   public void error(final String message, final Exception e, final int errorCode) {
      this.error(message, e, errorCode, null);
   }

   @Override
   public void error(final String message, final Exception e, final int errorCode, final LoggingEvent event) {
      if (e instanceof InterruptedIOException) {
         Thread.currentThread().interrupt();
      }

      LogLog.debug("FB: The following error reported: " + message, e);
      LogLog.debug("FB: INITIATING FALLBACK PROCEDURE.");
      if (this.loggers != null) {
         for (int i = 0; i < this.loggers.size(); i++) {
            Logger l = (Logger)this.loggers.elementAt(i);
            LogLog.debug("FB: Searching for [" + this.primary.getName() + "] in logger [" + l.getName() + "].");
            LogLog.debug("FB: Replacing [" + this.primary.getName() + "] by [" + this.backup.getName() + "] in logger [" + l.getName() + "].");
            l.removeAppender(this.primary);
            LogLog.debug("FB: Adding appender [" + this.backup.getName() + "] to logger " + l.getName());
            l.addAppender(this.backup);
         }
      }
   }

   @Override
   public void setAppender(final Appender primary) {
      LogLog.debug("FB: Setting primary appender to [" + primary.getName() + "].");
      this.primary = primary;
   }

   @Override
   public void setBackupAppender(final Appender backup) {
      LogLog.debug("FB: Setting backup appender to [" + backup.getName() + "].");
      this.backup = backup;
   }

   @Override
   public void setLogger(final Logger logger) {
      LogLog.debug("FB: Adding logger [" + logger.getName() + "].");
      if (this.loggers == null) {
         this.loggers = new Vector();
      }

      this.loggers.addElement(logger);
   }
}
