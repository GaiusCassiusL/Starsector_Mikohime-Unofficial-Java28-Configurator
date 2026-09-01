package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.impl.MementoLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

public interface LogEvent {
   LogEvent toImmutable();

   default LogEvent toMemento() {
      return new MementoLogEvent(this);
   }

   default LogEvent toMemento(final boolean includeLocation) {
      return new MementoLogEvent(this, includeLocation);
   }

   ReadOnlyStringMap getContextData();

   ContextStack getContextStack();

   String getLoggerFqcn();

   Level getLevel();

   String getLoggerName();

   Marker getMarker();

   Message getMessage();

   long getTimeMillis();

   Instant getInstant();

   StackTraceElement getSource();

   String getThreadName();

   long getThreadId();

   int getThreadPriority();

   Throwable getThrown();

   ThrowableProxy getThrownProxy();

   boolean isEndOfBatch();

   boolean isIncludeLocation();

   void setEndOfBatch(boolean endOfBatch);

   void setIncludeLocation(boolean locationRequired);

   long getNanoTime();
}
