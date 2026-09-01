package org.apache.logging.log4j.core.impl;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.ThreadNameCachingStrategy;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.util.StringMap;

public class ReusableLogEventFactory implements LogEventFactory {
   private static final ThreadNameCachingStrategy THREAD_NAME_CACHING_STRATEGY = ThreadNameCachingStrategy.create();
   private static final ThreadLocal<MutableLogEvent> mutableLogEventThreadLocal = new ThreadLocal<>();
   private final ContextDataInjector injector;
   private final Clock clock;
   private final NanoClock nanoClock;

   @Inject
   public ReusableLogEventFactory(final ContextDataInjector injector, final Clock clock, final NanoClock nanoClock) {
      this.injector = injector;
      this.clock = clock;
      this.nanoClock = nanoClock;
   }

   @Override
   public LogEvent createEvent(
      final String loggerName,
      final Marker marker,
      final String fqcn,
      final Level level,
      final Message message,
      final List<Property> properties,
      final Throwable t
   ) {
      return this.createEvent(loggerName, marker, fqcn, null, level, message, properties, t);
   }

   @Override
   public LogEvent createEvent(
      final String loggerName,
      final Marker marker,
      final String fqcn,
      final StackTraceElement location,
      final Level level,
      final Message message,
      final List<Property> properties,
      final Throwable t
   ) {
      MutableLogEvent result = getOrCreateMutableLogEvent();
      result.reserved = true;
      result.setLoggerName(loggerName);
      result.setMarker(marker);
      result.setLoggerFqcn(fqcn);
      result.setLevel(level == null ? Level.OFF : level);
      result.setMessage(message);
      result.initTime(this.clock, this.nanoClock);
      result.setThrown(t);
      result.setSource(location);
      result.setContextData(this.injector.injectContextData(properties, (StringMap)result.getContextData()));
      result.setContextStack((ContextStack)(ThreadContext.getDepth() == 0 ? ThreadContext.EMPTY_STACK : ThreadContext.cloneStack()));
      if (THREAD_NAME_CACHING_STRATEGY == ThreadNameCachingStrategy.UNCACHED) {
         result.setThreadName(Thread.currentThread().getName());
         result.setThreadPriority(Thread.currentThread().getPriority());
      }

      return result;
   }

   private static MutableLogEvent getOrCreateMutableLogEvent() {
      MutableLogEvent result = mutableLogEventThreadLocal.get();
      return result != null && !result.reserved ? result : createInstance(result);
   }

   private static MutableLogEvent createInstance(final MutableLogEvent existing) {
      MutableLogEvent result = new MutableLogEvent();
      result.setThreadId(Thread.currentThread().getId());
      result.setThreadName(Thread.currentThread().getName());
      result.setThreadPriority(Thread.currentThread().getPriority());
      if (existing == null) {
         mutableLogEventThreadLocal.set(result);
      }

      return result;
   }

   public static void release(final LogEvent logEvent) {
      if (logEvent instanceof MutableLogEvent) {
         MutableLogEvent mutableLogEvent = (MutableLogEvent)logEvent;
         mutableLogEvent.clear();
         mutableLogEvent.reserved = false;
      }
   }
}
