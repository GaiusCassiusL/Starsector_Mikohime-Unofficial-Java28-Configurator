package org.apache.logging.log4j.core.impl;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.di.Key;

public interface LogEventFactory {
   Key<LogEventFactory> KEY = new Key<LogEventFactory>() {};

   LogEvent createEvent(String loggerName, Marker marker, String fqcn, Level level, Message data, List<Property> properties, Throwable t);

   default LogEvent createEvent(
      String loggerName, Marker marker, String fqcn, StackTraceElement location, Level level, Message data, List<Property> properties, Throwable t
   ) {
      return this.createEvent(loggerName, marker, fqcn, level, data, properties, t);
   }
}
