package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;

public interface AsyncLoggerConfigDelegate {
   RingBufferAdmin createRingBufferAdmin(final String contextName, final String loggerConfigName);

   EventRoute getEventRoute(final Level level);

   void enqueueEvent(LogEvent event, AsyncLoggerConfig asyncLoggerConfig);

   boolean tryEnqueue(LogEvent event, AsyncLoggerConfig asyncLoggerConfig);

   void setLogEventFactory(LogEventFactory logEventFactory);
}
