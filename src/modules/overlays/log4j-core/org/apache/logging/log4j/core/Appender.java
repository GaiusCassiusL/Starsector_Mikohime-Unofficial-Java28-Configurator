package org.apache.logging.log4j.core;

public interface Appender extends LifeCycle {
   String ELEMENT_TYPE = "appender";
   Appender[] EMPTY_ARRAY = new Appender[0];

   void append(LogEvent event);

   String getName();

   Layout getLayout();

   boolean ignoreExceptions();

   ErrorHandler getHandler();

   void setHandler(ErrorHandler handler);

   default boolean requiresLocation() {
      return false;
   }
}
