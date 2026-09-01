package org.apache.logging.log4j.core.impl;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.util.StringMap;

public class DefaultLogEventFactory implements LogEventFactory {
   private final ContextDataInjector injector;
   private final Clock clock;
   private final NanoClock nanoClock;

   public static DefaultLogEventFactory newInstance() {
      Injector injector = DI.createInjector();
      injector.init();
      return (DefaultLogEventFactory)injector.getInstance(DefaultLogEventFactory.class);
   }

   @Inject
   public DefaultLogEventFactory(final ContextDataInjector injector, final Clock clock, final NanoClock nanoClock) {
      this.injector = injector;
      this.clock = clock;
      this.nanoClock = nanoClock;
   }

   private StringMap createContextData(final List<Property> properties) {
      return this.injector.injectContextData(properties, ContextDataFactory.createContextData());
   }

   @Override
   public LogEvent createEvent(
      final String loggerName,
      final Marker marker,
      final String fqcn,
      final Level level,
      final Message data,
      final List<Property> properties,
      final Throwable t
   ) {
      return Log4jLogEvent.newBuilder()
         .setNanoTime(this.nanoClock.nanoTime())
         .setClock(this.clock)
         .setLoggerName(loggerName)
         .setMarker(marker)
         .setLoggerFqcn(fqcn)
         .setLevel(level)
         .setMessage(data)
         .setContextData(this.createContextData(properties))
         .setThrown(t)
         .build();
   }

   @Override
   public LogEvent createEvent(
      final String loggerName,
      final Marker marker,
      final String fqcn,
      final StackTraceElement location,
      final Level level,
      final Message data,
      final List<Property> properties,
      final Throwable t
   ) {
      return Log4jLogEvent.newBuilder()
         .setNanoTime(this.nanoClock.nanoTime())
         .setClock(this.clock)
         .setLoggerName(loggerName)
         .setMarker(marker)
         .setLoggerFqcn(fqcn)
         .setSource(location)
         .setLevel(level)
         .setMessage(data)
         .setContextData(this.createContextData(properties))
         .setThrown(t)
         .build();
   }
}
