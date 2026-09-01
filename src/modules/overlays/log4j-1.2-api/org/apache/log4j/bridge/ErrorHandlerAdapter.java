package org.apache.log4j.bridge;

import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;

public class ErrorHandlerAdapter implements ErrorHandler {
   private final org.apache.log4j.spi.ErrorHandler errorHandler;

   public ErrorHandlerAdapter(final org.apache.log4j.spi.ErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
   }

   public org.apache.log4j.spi.ErrorHandler getHandler() {
      return this.errorHandler;
   }

   public void error(final String msg) {
      this.errorHandler.error(msg);
   }

   public void error(final String msg, final Throwable t) {
      if (t instanceof Exception) {
         this.errorHandler.error(msg, (Exception)t, 0);
      } else {
         this.errorHandler.error(msg);
      }
   }

   public void error(final String msg, final LogEvent event, final Throwable t) {
      if (t != null && !(t instanceof Exception)) {
         this.errorHandler.error(msg);
      } else {
         this.errorHandler.error(msg, (Exception)t, 0, new LogEventAdapter(event));
      }
   }
}
