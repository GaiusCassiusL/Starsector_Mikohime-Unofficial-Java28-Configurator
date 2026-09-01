package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.EventTranslatorVararg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.message.Message;

class AsyncLoggerDisruptor extends AbstractLifeCycle {
   private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
   private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;
   private final Object queueFullEnqueueLock = new Object();
   private volatile Disruptor<RingBufferLogEvent> disruptor;
   private String contextName;
   private final Supplier<AsyncWaitStrategyFactory> waitStrategyFactorySupplier;
   private boolean useThreadLocalTranslator = true;
   private long backgroundThreadId;
   private AsyncQueueFullPolicy asyncQueueFullPolicy;
   private int ringBufferSize;
   private WaitStrategy waitStrategy;

   AsyncLoggerDisruptor(final String contextName, final Supplier<AsyncWaitStrategyFactory> waitStrategyFactorySupplier) {
      this.contextName = contextName;
      this.waitStrategyFactorySupplier = Objects.requireNonNull(waitStrategyFactorySupplier, "waitStrategyFactorySupplier");
   }

   WaitStrategy getWaitStrategy() {
      return this.waitStrategy;
   }

   public String getContextName() {
      return this.contextName;
   }

   public void setContextName(final String name) {
      this.contextName = name;
   }

   Disruptor<RingBufferLogEvent> getDisruptor() {
      return this.disruptor;
   }

   @Override
   public synchronized void start() {
      if (this.disruptor != null) {
         LOGGER.trace("[{}] AsyncLoggerDisruptor not starting new disruptor for this context, using existing object.", this.contextName);
      } else if (this.isStarting()) {
         LOGGER.trace("[{}] AsyncLoggerDisruptor is already starting.", this.contextName);
      } else {
         this.setStarting();
         LOGGER.trace("[{}] AsyncLoggerDisruptor creating new disruptor for this context.", this.contextName);
         this.ringBufferSize = DisruptorUtil.calculateRingBufferSize(Log4jPropertyKey.ASYNC_LOGGER_RING_BUFFER_SIZE);
         AsyncWaitStrategyFactory factory = this.waitStrategyFactorySupplier.get();
         this.waitStrategy = DisruptorUtil.createWaitStrategy(Log4jPropertyKey.ASYNC_LOGGER_WAIT_STRATEGY, factory);
         ThreadFactory threadFactory = new Log4jThreadFactory("AsyncLogger[" + this.contextName + "]", true, 5) {
            @Override
            public Thread newThread(final Runnable r) {
               Thread result = super.newThread(r);
               AsyncLoggerDisruptor.this.backgroundThreadId = result.getId();
               return result;
            }
         };
         this.asyncQueueFullPolicy = AsyncQueueFullPolicyFactory.create();
         this.disruptor = new Disruptor(RingBufferLogEvent.FACTORY, this.ringBufferSize, threadFactory, ProducerType.MULTI, this.waitStrategy);
         ExceptionHandler<RingBufferLogEvent> errorHandler = DisruptorUtil.getAsyncLoggerExceptionHandler();
         this.disruptor.setDefaultExceptionHandler(errorHandler);
         RingBufferLogEventHandler[] handlers = new RingBufferLogEventHandler[]{new RingBufferLogEventHandler()};
         this.disruptor.handleEventsWith(handlers);
         LOGGER.debug(
            "[{}] Starting AsyncLogger disruptor for this context with ringbufferSize={}, waitStrategy={}, exceptionHandler={}...",
            this.contextName,
            this.disruptor.getRingBuffer().getBufferSize(),
            this.waitStrategy.getClass().getSimpleName(),
            errorHandler
         );
         this.disruptor.start();
         LOGGER.trace("[{}] AsyncLoggers use a {} translator", this.contextName, this.useThreadLocalTranslator ? "threadlocal" : "vararg");
         super.start();
      }
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      Disruptor<RingBufferLogEvent> temp = this.getDisruptor();
      if (temp == null) {
         LOGGER.trace("[{}] AsyncLoggerDisruptor: disruptor for this context already shut down.", this.contextName);
         return true;
      }

      this.setStopping();
      LOGGER.debug("[{}] AsyncLoggerDisruptor: shutting down disruptor for this context.", this.contextName);
      this.disruptor = null;

      for (int i = 0; hasBacklog(temp) && i < 200; i++) {
         try {
            Thread.sleep(50L);
         } catch (InterruptedException var8) {
         }
      }

      try {
         temp.shutdown(timeout, timeUnit);
      } catch (TimeoutException e) {
         LOGGER.warn("[{}] AsyncLoggerDisruptor: shutdown timed out after {} {}", this.contextName, timeout, timeUnit);
         temp.halt();
      }

      LOGGER.trace("[{}] AsyncLoggerDisruptor: disruptor has been shut down.", this.contextName);
      if (DiscardingAsyncQueueFullPolicy.getDiscardCount(this.asyncQueueFullPolicy) > 0L) {
         LOGGER.trace(
            "AsyncLoggerDisruptor: {} discarded {} events.",
            this.asyncQueueFullPolicy,
            DiscardingAsyncQueueFullPolicy.getDiscardCount(this.asyncQueueFullPolicy)
         );
      }

      this.setStopped();
      return true;
   }

   private static boolean hasBacklog(final Disruptor<?> theDisruptor) {
      RingBuffer<?> ringBuffer = theDisruptor.getRingBuffer();
      return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
   }

   public RingBufferAdmin createRingBufferAdmin(final String jmxContextName) {
      RingBuffer<RingBufferLogEvent> ring = this.disruptor == null ? null : this.disruptor.getRingBuffer();
      return RingBufferAdmin.forAsyncLogger(ring, jmxContextName);
   }

   EventRoute getEventRoute(final Level logLevel) {
      int remainingCapacity = this.remainingDisruptorCapacity();
      return remainingCapacity < 0 ? EventRoute.DISCARD : this.asyncQueueFullPolicy.getRoute(this.backgroundThreadId, logLevel);
   }

   private int remainingDisruptorCapacity() {
      Disruptor<RingBufferLogEvent> temp = this.disruptor;
      return this.hasLog4jBeenShutDown(temp) ? -1 : (int)temp.getRingBuffer().remainingCapacity();
   }

   private boolean hasLog4jBeenShutDown(final Disruptor<RingBufferLogEvent> aDisruptor) {
      if (aDisruptor == null) {
         LOGGER.warn("Ignoring log event after log4j was shut down");
         return true;
      } else {
         return false;
      }
   }

   boolean tryPublish(final RingBufferLogEventTranslator translator) {
      try {
         return this.disruptor.getRingBuffer().tryPublishEvent(translator);
      } catch (NullPointerException npe) {
         this.logWarningOnNpeFromDisruptorPublish(translator);
         return false;
      }
   }

   void enqueueLogMessageWhenQueueFull(final RingBufferLogEventTranslator translator) {
      try {
         if (this.synchronizeEnqueueWhenQueueFull()) {
            synchronized (this.queueFullEnqueueLock) {
               this.disruptor.publishEvent(translator);
            }
         } else {
            this.disruptor.publishEvent(translator);
         }
      } catch (NullPointerException npe) {
         this.logWarningOnNpeFromDisruptorPublish(translator);
      }
   }

   void enqueueLogMessageWhenQueueFull(
      final EventTranslatorVararg<RingBufferLogEvent> translator,
      final AsyncLogger asyncLogger,
      final StackTraceElement location,
      final String fqcn,
      final Level level,
      final Marker marker,
      final Message msg,
      final Throwable thrown
   ) {
      try {
         if (this.synchronizeEnqueueWhenQueueFull()) {
            synchronized (this.queueFullEnqueueLock) {
               this.disruptor.getRingBuffer().publishEvent(translator, new Object[]{asyncLogger, location, fqcn, level, marker, msg, thrown});
            }
         } else {
            this.disruptor.getRingBuffer().publishEvent(translator, new Object[]{asyncLogger, location, fqcn, level, marker, msg, thrown});
         }
      } catch (NullPointerException npe) {
         this.logWarningOnNpeFromDisruptorPublish(level, fqcn, msg, thrown);
      }
   }

   private boolean synchronizeEnqueueWhenQueueFull() {
      return DisruptorUtil.ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL
         && this.backgroundThreadId != Thread.currentThread().getId()
         && !(Thread.currentThread() instanceof Log4jThread);
   }

   private void logWarningOnNpeFromDisruptorPublish(final RingBufferLogEventTranslator translator) {
      this.logWarningOnNpeFromDisruptorPublish(translator.level, translator.loggerName, translator.message, translator.thrown);
   }

   private void logWarningOnNpeFromDisruptorPublish(final Level level, final String fqcn, final Message msg, final Throwable thrown) {
      LOGGER.warn(
         "[{}] Ignoring log event after log4j was shut down: {} [{}] {}{}",
         this.contextName,
         level,
         fqcn,
         msg.getFormattedMessage(),
         thrown == null ? "" : Throwables.toStringList(thrown)
      );
   }

   public boolean isUseThreadLocals() {
      return this.useThreadLocalTranslator;
   }

   public void setUseThreadLocals(final boolean allow) {
      this.useThreadLocalTranslator = allow;
      LOGGER.trace("[{}] AsyncLoggers have been modified to use a {} translator", this.contextName, this.useThreadLocalTranslator ? "threadlocal" : "vararg");
   }
}
