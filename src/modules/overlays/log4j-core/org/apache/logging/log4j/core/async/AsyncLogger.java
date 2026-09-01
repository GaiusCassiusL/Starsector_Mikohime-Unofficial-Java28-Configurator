package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.EventTranslatorVararg;
import com.lmax.disruptor.dsl.Disruptor;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.StringMap;

public class AsyncLogger extends Logger implements EventTranslatorVararg<RingBufferLogEvent> {
   private static final StatusLogger LOGGER = StatusLogger.getLogger();
   private final Clock clock;
   private final ContextDataInjector contextDataInjector;
   private static final ThreadNameCachingStrategy THREAD_NAME_CACHING_STRATEGY = ThreadNameCachingStrategy.create();
   private final ThreadLocal<RingBufferLogEventTranslator> threadLocalTranslator = new ThreadLocal<>();
   private final AsyncLoggerDisruptor loggerDisruptor;
   private volatile boolean includeLocation;
   private volatile NanoClock nanoClock;
   private final AsyncLogger.TranslatorType threadLocalTranslatorType = new AsyncLogger.TranslatorType() {
      @Override
      void log(final String fqcn, final StackTraceElement location, final Level level, final Marker marker, final Message message, final Throwable thrown) {
         AsyncLogger.this.logWithThreadLocalTranslator(fqcn, location, level, marker, message, thrown);
      }

      @Override
      void log(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
         AsyncLogger.this.logWithThreadLocalTranslator(fqcn, level, marker, message, thrown);
      }
   };
   private final AsyncLogger.TranslatorType varargTranslatorType = new AsyncLogger.TranslatorType() {
      @Override
      void log(final String fqcn, final StackTraceElement location, final Level level, final Marker marker, final Message message, final Throwable thrown) {
         AsyncLogger.this.logWithVarargTranslator(fqcn, location, level, marker, message, thrown);
      }

      @Override
      void log(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
         AsyncLogger.this.logWithVarargTranslator(fqcn, level, marker, message, thrown);
      }
   };

   public AsyncLogger(final LoggerContext context, final String name, final MessageFactory messageFactory, final AsyncLoggerDisruptor loggerDisruptor) {
      super(context, name, messageFactory);
      this.loggerDisruptor = loggerDisruptor;
      this.includeLocation = this.privateConfig.loggerConfig.isIncludeLocation();
      Configuration configuration = context.getConfiguration();
      this.nanoClock = configuration.getNanoClock();
      this.clock = configuration.getComponent(Clock.KEY);
      this.contextDataInjector = configuration.getComponent(ContextDataInjector.KEY);
   }

   @Override
   protected void updateConfiguration(final Configuration newConfig) {
      this.nanoClock = newConfig.getNanoClock();
      this.includeLocation = newConfig.getLoggerConfig(this.name).isIncludeLocation();
      super.updateConfiguration(newConfig);
   }

   NanoClock getNanoClock() {
      return this.nanoClock;
   }

   private RingBufferLogEventTranslator getCachedTranslator() {
      RingBufferLogEventTranslator result = this.threadLocalTranslator.get();
      if (result == null) {
         result = new RingBufferLogEventTranslator();
         this.threadLocalTranslator.set(result);
      }

      return result;
   }

   @Override
   public void logMessage(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
      this.getTranslatorType().log(fqcn, level, marker, message, thrown);
   }

   @Override
   public void log(
      final Level level, final Marker marker, final String fqcn, final StackTraceElement location, final Message message, final Throwable throwable
   ) {
      this.getTranslatorType().log(fqcn, location, level, marker, message, throwable);
   }

   private AsyncLogger.TranslatorType getTranslatorType() {
      return this.loggerDisruptor.isUseThreadLocals() ? this.threadLocalTranslatorType : this.varargTranslatorType;
   }

   private boolean isReused(final Message message) {
      return message instanceof ReusableMessage;
   }

   private void logWithThreadLocalTranslator(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
      RingBufferLogEventTranslator translator = this.getCachedTranslator();
      this.initTranslator(translator, fqcn, level, marker, message, thrown);
      this.initTranslatorThreadValues(translator);
      this.publish(translator);
   }

   private void logWithThreadLocalTranslator(
      final String fqcn, final StackTraceElement location, final Level level, final Marker marker, final Message message, final Throwable thrown
   ) {
      RingBufferLogEventTranslator translator = this.getCachedTranslator();
      this.initTranslator(translator, fqcn, location, level, marker, message, thrown);
      this.initTranslatorThreadValues(translator);

      try {
         this.publish(translator);
      } catch (Throwable ex) {
         translator.clear();
         throw ex;
      }
   }

   private void publish(final RingBufferLogEventTranslator translator) {
      if (!this.loggerDisruptor.tryPublish(translator)) {
         this.handleRingBufferFull(translator);
      }
   }

   private void handleRingBufferFull(final RingBufferLogEventTranslator translator) {
      if (AbstractLogger.getRecursionDepth() > 1) {
         AsyncQueueFullMessageUtil.logWarningToStatusLogger();
         this.logMessageInCurrentThread(translator.fqcn, translator.level, translator.marker, translator.message, translator.thrown);
         translator.clear();
      } else {
         EventRoute eventRoute = this.loggerDisruptor.getEventRoute(translator.level);
         switch (eventRoute) {
            case ENQUEUE:
               this.loggerDisruptor.enqueueLogMessageWhenQueueFull(translator);
               break;
            case SYNCHRONOUS:
               this.logMessageInCurrentThread(translator.fqcn, translator.level, translator.marker, translator.message, translator.thrown);
               translator.clear();
               break;
            case DISCARD:
               translator.clear();
               break;
            default:
               throw new IllegalStateException("Unknown EventRoute " + eventRoute);
         }
      }
   }

   private void initTranslator(
      final RingBufferLogEventTranslator translator,
      final String fqcn,
      final StackTraceElement location,
      final Level level,
      final Marker marker,
      final Message message,
      final Throwable thrown
   ) {
      translator.setBasicValues(
         this,
         this.name,
         marker,
         fqcn,
         level,
         message,
         thrown,
         ThreadContext.getImmutableStack(),
         location,
         this.clock,
         this.nanoClock,
         this.contextDataInjector
      );
   }

   private void initTranslator(
      final RingBufferLogEventTranslator translator, final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown
   ) {
      translator.setBasicValues(
         this,
         this.name,
         marker,
         fqcn,
         level,
         message,
         thrown,
         ThreadContext.getImmutableStack(),
         this.calcLocationIfRequested(fqcn),
         this.clock,
         this.nanoClock,
         this.contextDataInjector
      );
   }

   private void initTranslatorThreadValues(final RingBufferLogEventTranslator translator) {
      if (THREAD_NAME_CACHING_STRATEGY == ThreadNameCachingStrategy.UNCACHED) {
         translator.updateThreadValues();
      }
   }

   private StackTraceElement calcLocationIfRequested(final String fqcn) {
      return this.includeLocation ? StackLocatorUtil.calcLocation(fqcn) : null;
   }

   private void logWithVarargTranslator(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
      Disruptor<RingBufferLogEvent> disruptor = this.loggerDisruptor.getDisruptor();
      if (disruptor == null) {
         LOGGER.error("Ignoring log event after Log4j has been shut down.");
      } else {
         if (!this.isReused(message)) {
            InternalAsyncUtil.makeMessageImmutable(message);
         }

         StackTraceElement location = null;
         if (!disruptor.getRingBuffer()
            .tryPublishEvent(this, new Object[]{this, location = this.calcLocationIfRequested(fqcn), fqcn, level, marker, message, thrown})) {
            this.handleRingBufferFull(location, fqcn, level, marker, message, thrown);
         }
      }
   }

   private void logWithVarargTranslator(
      final String fqcn, final StackTraceElement location, final Level level, final Marker marker, final Message message, final Throwable thrown
   ) {
      Disruptor<RingBufferLogEvent> disruptor = this.loggerDisruptor.getDisruptor();
      if (disruptor == null) {
         LOGGER.error("Ignoring log event after Log4j has been shut down.");
      } else {
         if (!this.isReused(message)) {
            InternalAsyncUtil.makeMessageImmutable(message);
         }

         if (!disruptor.getRingBuffer().tryPublishEvent(this, new Object[]{this, location, fqcn, level, marker, message, thrown})) {
            this.handleRingBufferFull(location, fqcn, level, marker, message, thrown);
         }
      }
   }

   public void translateTo(final RingBufferLogEvent event, final long sequence, final Object... args) {
      AsyncLogger asyncLogger = (AsyncLogger)args[0];
      StackTraceElement location = (StackTraceElement)args[1];
      String fqcn = (String)args[2];
      Level level = (Level)args[3];
      Marker marker = (Marker)args[4];
      Message message = (Message)args[5];
      Throwable thrown = (Throwable)args[6];
      ContextStack contextStack = ThreadContext.getImmutableStack();
      Thread currentThread = Thread.currentThread();
      String threadName = THREAD_NAME_CACHING_STRATEGY.getThreadName();
      event.setValues(
         asyncLogger,
         asyncLogger.getName(),
         marker,
         fqcn,
         level,
         message,
         thrown,
         this.contextDataInjector.injectContextData(null, (StringMap)event.getContextData()),
         contextStack,
         currentThread.getId(),
         threadName,
         currentThread.getPriority(),
         location,
         this.clock,
         this.nanoClock
      );
   }

   void logMessageInCurrentThread(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
      ReliabilityStrategy strategy = this.privateConfig.loggerConfig.getReliabilityStrategy();
      strategy.log(this, this.getName(), fqcn, marker, level, message, thrown);
   }

   private void handleRingBufferFull(
      final StackTraceElement location, final String fqcn, final Level level, final Marker marker, final Message msg, final Throwable thrown
   ) {
      if (AbstractLogger.getRecursionDepth() > 1) {
         AsyncQueueFullMessageUtil.logWarningToStatusLogger();
         this.logMessageInCurrentThread(fqcn, level, marker, msg, thrown);
      } else {
         EventRoute eventRoute = this.loggerDisruptor.getEventRoute(level);
         switch (eventRoute) {
            case ENQUEUE:
               this.loggerDisruptor.enqueueLogMessageWhenQueueFull(this, this, location, fqcn, level, marker, msg, thrown);
               break;
            case SYNCHRONOUS:
               this.logMessageInCurrentThread(fqcn, level, marker, msg, thrown);
            case DISCARD:
               break;
            default:
               throw new IllegalStateException("Unknown EventRoute " + eventRoute);
         }
      }
   }

   public void actualAsyncLog(final RingBufferLogEvent event) {
      LoggerConfig privateConfigLoggerConfig = this.privateConfig.loggerConfig;
      List<Property> properties = privateConfigLoggerConfig.getPropertyList();
      if (properties != null) {
         this.onPropertiesPresent(event, properties);
      }

      privateConfigLoggerConfig.getReliabilityStrategy().log(this, event);
   }

   private void onPropertiesPresent(final RingBufferLogEvent event, final List<Property> properties) {
      StringMap contextData = getContextData(event);
      int i = 0;

      for (int size = properties.size(); i < size; i++) {
         Property prop = properties.get(i);
         if (contextData.getValue(prop.getName()) == null) {
            String value = prop.isValueNeedsLookup() ? this.privateConfig.config.getStrSubstitutor().replace(event, prop.getValue()) : prop.getValue();
            contextData.putValue(prop.getName(), value);
         }
      }

      event.setContextData(contextData);
   }

   private static StringMap getContextData(final RingBufferLogEvent event) {
      StringMap contextData = (StringMap)event.getContextData();
      if (contextData.isFrozen()) {
         StringMap temp = ContextDataFactory.createContextData();
         temp.putAll(contextData);
         return temp;
      } else {
         return contextData;
      }
   }

   AsyncLoggerDisruptor getAsyncLoggerDisruptor() {
      return this.loggerDisruptor;
   }

   abstract class TranslatorType {
      abstract void log(
         final String fqcn, final StackTraceElement location, final Level level, final Marker marker, final Message message, final Throwable thrown
      );

      abstract void log(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown);
   }
}
