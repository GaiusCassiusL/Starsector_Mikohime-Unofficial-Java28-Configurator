package org.apache.logging.log4j.core;

public interface ErrorHandler {
   void error(String msg);

   void error(String msg, Throwable t);

   void error(String msg, LogEvent event, Throwable t);
}
