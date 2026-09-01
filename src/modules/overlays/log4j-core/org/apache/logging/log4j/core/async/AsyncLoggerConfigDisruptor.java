package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.impl.ReusableLogEventFactory;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.message.ReusableMessage;

public class AsyncLoggerConfigDisruptor extends AbstractLifeCycle implements AsyncLoggerConfigDelegate {
   private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;
   private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
   private static final EventFactory<AsyncLoggerConfigDisruptor.Log4jEventWrapper> FACTORY = AsyncLoggerConfigDisruptor.Log4jEventWrapper::new;
   private static final EventFactory<AsyncLoggerConfigDisruptor.Log4jEventWrapper> MUTABLE_FACTORY = () -> new AsyncLoggerConfigDisruptor.Log4jEventWrapper(
      new MutableLogEvent()
   );
   private static final EventTranslatorTwoArg<AsyncLoggerConfigDisruptor.Log4jEventWrapper, LogEvent, AsyncLoggerConfig> TRANSLATOR = (ringBufferElement, sequence, logEvent, loggerConfig) -> {
      ringBufferElement.event = logEvent;
      ringBufferElement.loggerConfig = loggerConfig;
   };
   private static final EventTranslatorTwoArg<AsyncLoggerConfigDisruptor.Log4jEventWrapper, LogEvent, AsyncLoggerConfig> MUTABLE_TRANSLATOR = (ringBufferElement, sequence, logEvent, loggerConfig) -> {
      ((MutableLogEvent)ringBufferElement.event).initFrom(logEvent);
      ringBufferElement.loggerConfig = loggerConfig;
   };
   private int ringBufferSize;
   private AsyncQueueFullPolicy asyncQueueFullPolicy;
   private Boolean mutable = Boolean.FALSE;
   private volatile Disruptor<AsyncLoggerConfigDisruptor.Log4jEventWrapper> disruptor;
   private long backgroundThreadId;
   private EventFactory<AsyncLoggerConfigDisruptor.Log4jEventWrapper> factory;
   private EventTranslatorTwoArg<AsyncLoggerConfigDisruptor.Log4jEventWrapper, LogEvent, AsyncLoggerConfig> translator;
   private volatile boolean alreadyLoggedWarning;
   private final AsyncWaitStrategyFactory asyncWaitStrategyFactory;
   private WaitStrategy waitStrategy;
   private final Object queueFullEnqueueLock = new Object();

   public AsyncLoggerConfigDisruptor(final AsyncWaitStrategyFactory asyncWaitStrategyFactory) {
      this.asyncWaitStrategyFactory = asyncWaitStrategyFactory;
   }

   WaitStrategy getWaitStrategy() {
      return this.waitStrategy;
   }

   @Override
   public void setLogEventFactory(final LogEventFactory logEventFactory) {
      this.mutable = this.mutable || logEventFactory instanceof ReusableLogEventFactory;
   }

   @Override
   public synchronized void start() {
      if (this.disruptor != null) {
         LOGGER.trace("AsyncLoggerConfigDisruptor not starting new disruptor for this configuration, using existing object.");
      } else {
         LOGGER.trace("AsyncLoggerConfigDisruptor creating new disruptor for this configuration.");
         this.ringBufferSize = DisruptorUtil.calculateRingBufferSize(Log4jPropertyKey.ASYNC_CONFIG_RING_BUFFER_SIZE);
         this.waitStrategy = DisruptorUtil.createWaitStrategy(Log4jPropertyKey.ASYNC_CONFIG_WAIT_STRATEGY, this.asyncWaitStrategyFactory);
         ThreadFactory threadFactory = new Log4jThreadFactory("AsyncLoggerConfig", true, 5) {
            @Override
            public Thread newThread(final Runnable r) {
               Thread result = super.newThread(r);
               AsyncLoggerConfigDisruptor.this.backgroundThreadId = result.getId();
               return result;
            }
         };
         this.asyncQueueFullPolicy = AsyncQueueFullPolicyFactory.create();
         this.translator = this.mutable ? MUTABLE_TRANSLATOR : TRANSLATOR;
         this.factory = this.mutable ? MUTABLE_FACTORY : FACTORY;
         this.disruptor = new Disruptor(this.factory, this.ringBufferSize, threadFactory, ProducerType.MULTI, this.waitStrategy);
         ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper> errorHandler = DisruptorUtil.getAsyncLoggerConfigExceptionHandler();
         this.disruptor.setDefaultExceptionHandler(errorHandler);
         AsyncLoggerConfigDisruptor.Log4jEventWrapperHandler[] handlers = new AsyncLoggerConfigDisruptor.Log4jEventWrapperHandler[]{
            new AsyncLoggerConfigDisruptor.Log4jEventWrapperHandler()
         };
         this.disruptor.handleEventsWith(handlers);
         LOGGER.debug(
            "Starting AsyncLoggerConfig disruptor for this configuration with ringbufferSize={}, waitStrategy={}, exceptionHandler={}...",
            this.disruptor.getRingBuffer().getBufferSize(),
            this.waitStrategy.getClass().getSimpleName(),
            errorHandler
         );
         this.disruptor.start();
         super.start();
      }
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      Disruptor<AsyncLoggerConfigDisruptor.Log4jEventWrapper> temp = this.disruptor;
      if (temp == null) {
         LOGGER.trace("AsyncLoggerConfigDisruptor: disruptor for this configuration already shut down.");
         return true;
      }

      this.setStopping();
      LOGGER.trace("AsyncLoggerConfigDisruptor: shutting down disruptor for this configuration.");
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
         LOGGER.warn("AsyncLoggerConfigDisruptor: shutdown timed out after {} {}", timeout, timeUnit);
         temp.halt();
      }

      LOGGER.trace("AsyncLoggerConfigDisruptor: disruptor has been shut down.");
      if (DiscardingAsyncQueueFullPolicy.getDiscardCount(this.asyncQueueFullPolicy) > 0L) {
         LOGGER.trace(
            "AsyncLoggerConfigDisruptor: {} discarded {} events.",
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

   @Override
   public EventRoute getEventRoute(final Level logLevel) {
      int remainingCapacity = this.remainingDisruptorCapacity();
      return remainingCapacity < 0 ? EventRoute.DISCARD : this.asyncQueueFullPolicy.getRoute(this.backgroundThreadId, logLevel);
   }

   private int remainingDisruptorCapacity() {
      Disruptor<AsyncLoggerConfigDisruptor.Log4jEventWrapper> temp = this.disruptor;
      return this.hasLog4jBeenShutDown(temp) ? -1 : (int)temp.getRingBuffer().remainingCapacity();
   }

   private boolean hasLog4jBeenShutDown(final Disruptor<AsyncLoggerConfigDisruptor.Log4jEventWrapper> aDisruptor) {
      if (aDisruptor == null) {
         LOGGER.warn("Ignoring log event after log4j was shut down");
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void enqueueEvent(final LogEvent event, final AsyncLoggerConfig asyncLoggerConfig) {
      try {
         LogEvent logEvent = this.prepareEvent(event);
         this.enqueue(logEvent, asyncLoggerConfig);
      } catch (NullPointerException npe) {
         LOGGER.warn(
            "Ignoring log event after log4j was shut down: {} [{}] {}",
            event.getLevel(),
            event.getLoggerName(),
            event.getMessage().getFormattedMessage() + (event.getThrown() == null ? "" : Throwables.toStringList(event.getThrown()))
         );
      }
   }

   private LogEvent prepareEvent(final LogEvent event) {
      LogEvent logEvent = this.ensureImmutable(event);
      if (logEvent.getMessage() instanceof ReusableMessage) {
         if (logEvent instanceof Log4jLogEvent) {
            ((Log4jLogEvent)logEvent).makeMessageImmutable();
         } else if (logEvent instanceof MutableLogEvent) {
            if (this.translator != MUTABLE_TRANSLATOR) {
               logEvent = logEvent.toImmutable();
            }
         } else {
            this.showWarningAboutCustomLogEventWithReusableMessage(logEvent);
         }
      } else {
         InternalAsyncUtil.makeMessageImmutable(logEvent.getMessage());
      }

      return logEvent;
   }

   private void showWarningAboutCustomLogEventWithReusableMessage(final LogEvent logEvent) {
      if (!this.alreadyLoggedWarning) {
         LOGGER.warn(
            "Custom log event of type {} contains a mutable message of type {}. AsyncLoggerConfig does not know how to make an immutable copy of this message. This may result in ConcurrentModificationExceptions or incorrect log messages if the application modifies objects in the message while the background thread is writing it to the appenders.",
            logEvent.getClass().getName(),
            logEvent.getMessage().getClass().getName()
         );
         this.alreadyLoggedWarning = true;
      }
   }

   private void enqueue(final LogEvent logEvent, final AsyncLoggerConfig asyncLoggerConfig) {
      if (this.synchronizeEnqueueWhenQueueFull()) {
         synchronized (this.queueFullEnqueueLock) {
            this.disruptor.getRingBuffer().publishEvent(this.translator, logEvent, asyncLoggerConfig);
         }
      } else {
         this.disruptor.getRingBuffer().publishEvent(this.translator, logEvent, asyncLoggerConfig);
      }
   }

   private boolean synchronizeEnqueueWhenQueueFull() {
      return DisruptorUtil.ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL
         && this.backgroundThreadId != Thread.currentThread().getId()
         && !(Thread.currentThread() instanceof Log4jThread);
   }

   @Override
   public boolean tryEnqueue(final LogEvent event, final AsyncLoggerConfig asyncLoggerConfig) {
      LogEvent logEvent = this.prepareEvent(event);
      return this.disruptor.getRingBuffer().tryPublishEvent(this.translator, logEvent, asyncLoggerConfig);
   }

   private LogEvent ensureImmutable(final LogEvent event) {
      LogEvent result = event;
      if (event instanceof RingBufferLogEvent) {
         result = event.toMemento();
      }

      return result;
   }

   @Override
   public RingBufferAdmin createRingBufferAdmin(final String contextName, final String loggerConfigName) {
      return RingBufferAdmin.forAsyncLoggerConfig(this.disruptor.getRingBuffer(), contextName, loggerConfigName);
   }

   public static class Log4jEventWrapper {
      private AsyncLoggerConfig loggerConfig;
      private LogEvent event;

      public Log4jEventWrapper() {
      }

      public Log4jEventWrapper(final MutableLogEvent mutableLogEvent) {
         this.event = mutableLogEvent;
      }

      public void clear() {
         this.loggerConfig = null;
         if (this.event instanceof MutableLogEvent) {
            ((MutableLogEvent)this.event).clear();
         } else {
            this.event = null;
         }
      }

      @Override
      public String toString() {
         return String.valueOf(this.event);
      }
   }

   private static class Log4jEventWrapperHandler implements SequenceReportingEventHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper> {
      private static final int NOTIFY_PROGRESS_THRESHOLD = 50;
      private Sequence sequenceCallback;
      private int counter;

      public void setSequenceCallback(final Sequence sequenceCallback) {
         this.sequenceCallback = sequenceCallback;
      }

      public void onEvent(final AsyncLoggerConfigDisruptor.Log4jEventWrapper event, final long sequence, final boolean endOfBatch) throws Exception {
         event.event.setEndOfBatch(endOfBatch);
         event.loggerConfig.logToAsyncLoggerConfigsOnCurrentThread(event.event);
         event.clear();
         this.notifyIntermediateProgress(sequence);
      }

      private void notifyIntermediateProgress(final long sequence) {
         if (++this.counter > 50) {
            this.sequenceCallback.set(sequence);
            this.counter = 0;
         }
      }
   }
}
