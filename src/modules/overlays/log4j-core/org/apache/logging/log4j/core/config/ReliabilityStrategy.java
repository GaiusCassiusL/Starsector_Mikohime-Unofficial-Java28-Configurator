package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;

public interface ReliabilityStrategy {
   void log(Supplier<LoggerConfig> reconfigured, String loggerName, String fqcn, Marker marker, Level level, Message data, Throwable t);

   default void log(
      final Supplier<LoggerConfig> reconfigured,
      final String loggerName,
      final String fqcn,
      final StackTraceElement location,
      final Marker marker,
      final Level level,
      final Message data,
      final Throwable t
   ) {
   }

   void log(Supplier<LoggerConfig> reconfigured, LogEvent event);

   LoggerConfig getActiveLoggerConfig(Supplier<LoggerConfig> next);

   void afterLogEvent();

   void beforeStopAppenders();

   void beforeStopConfiguration(Configuration configuration);
}
