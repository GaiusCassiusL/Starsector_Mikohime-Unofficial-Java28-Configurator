package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.EventTranslator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringMap;

public class RingBufferLogEventTranslator implements EventTranslator<RingBufferLogEvent> {
   private ContextDataInjector contextDataInjector;
   private AsyncLogger asyncLogger;
   String loggerName;
   protected Marker marker;
   protected String fqcn;
   protected Level level;
   protected Message message;
   protected Throwable thrown;
   private ContextStack contextStack;
   private long threadId = Thread.currentThread().getId();
   private String threadName = Thread.currentThread().getName();
   private int threadPriority = Thread.currentThread().getPriority();
   private StackTraceElement location;
   private Clock clock;
   private NanoClock nanoClock;

   public void translateTo(final RingBufferLogEvent event, final long sequence) {
      try {
         event.setValues(
            this.asyncLogger,
            this.loggerName,
            this.marker,
            this.fqcn,
            this.level,
            this.message,
            this.thrown,
            this.contextDataInjector.injectContextData(null, (StringMap)event.getContextData()),
            this.contextStack,
            this.threadId,
            this.threadName,
            this.threadPriority,
            this.location,
            this.clock,
            this.nanoClock
         );
      } finally {
         this.clear();
      }
   }

   void clear() {
      this.setBasicValues(null, null, null, null, null, null, null, null, null, null, null, null);
   }

   public void setBasicValues(
      final AsyncLogger anAsyncLogger,
      final String aLoggerName,
      final Marker aMarker,
      final String theFqcn,
      final Level aLevel,
      final Message msg,
      final Throwable aThrowable,
      final ContextStack aContextStack,
      final StackTraceElement aLocation,
      final Clock aClock,
      final NanoClock aNanoClock,
      final ContextDataInjector aContextDataInjector
   ) {
      this.asyncLogger = anAsyncLogger;
      this.loggerName = aLoggerName;
      this.marker = aMarker;
      this.fqcn = theFqcn;
      this.level = aLevel;
      this.message = msg;
      this.thrown = aThrowable;
      this.contextStack = aContextStack;
      this.location = aLocation;
      this.clock = aClock;
      this.nanoClock = aNanoClock;
      this.contextDataInjector = aContextDataInjector;
   }

   public void updateThreadValues() {
      Thread currentThread = Thread.currentThread();
      this.threadId = currentThread.getId();
      this.threadName = currentThread.getName();
      this.threadPriority = currentThread.getPriority();
   }
}
