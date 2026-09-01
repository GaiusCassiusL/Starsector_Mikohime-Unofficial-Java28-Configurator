package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.WaitStrategy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyKey;

final class DisruptorUtil {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final int RINGBUFFER_MIN_SIZE = 128;
   private static final int RINGBUFFER_DEFAULT_SIZE = 262144;
   private static final int RINGBUFFER_NO_GC_DEFAULT_SIZE = 4096;
   static final boolean ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = PropertiesUtil.getProperties()
      .getBooleanProperty(Log4jPropertyKey.ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL, true);
   static final boolean ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = PropertiesUtil.getProperties()
      .getBooleanProperty(Log4jPropertyKey.ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL, true);

   private DisruptorUtil() {
   }

   static WaitStrategy createWaitStrategy(final PropertyKey key, final AsyncWaitStrategyFactory asyncWaitStrategyFactory) {
      if (asyncWaitStrategyFactory == null) {
         LOGGER.debug("No AsyncWaitStrategyFactory was configured in the configuration, using default factory...");
         return new DefaultAsyncWaitStrategyFactory(key).createWaitStrategy();
      } else {
         LOGGER.debug("Using configured AsyncWaitStrategyFactory {}", asyncWaitStrategyFactory.getClass().getName());
         return asyncWaitStrategyFactory.createWaitStrategy();
      }
   }

   static int calculateRingBufferSize(final PropertyKey key) {
      int ringBufferSize = Constants.isThreadLocalsEnabled() ? 4096 : 262144;
      String userPreferredRBSize = PropertiesUtil.getProperties().getStringProperty(key, String.valueOf(ringBufferSize));

      try {
         int size = Integer.parseInt(userPreferredRBSize);
         if (size < 128) {
            size = 128;
            LOGGER.warn("Invalid RingBufferSize {}, using minimum size {}.", userPreferredRBSize, 128);
         }

         ringBufferSize = size;
      } catch (Exception ex) {
         LOGGER.warn("Invalid RingBufferSize {}, using default size {}.", userPreferredRBSize, ringBufferSize);
      }

      return Integers.ceilingNextPowerOfTwo(ringBufferSize);
   }

   static ExceptionHandler<RingBufferLogEvent> getAsyncLoggerExceptionHandler() {
      String cls = PropertiesUtil.getProperties().getStringProperty(Log4jPropertyKey.ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME);
      if (cls == null) {
         return new AsyncLoggerDefaultExceptionHandler();
      }

      try {
         Class<? extends ExceptionHandler<RingBufferLogEvent>> klass = (Class<? extends ExceptionHandler<RingBufferLogEvent>>)Loader.loadClass(cls);
         return (ExceptionHandler<RingBufferLogEvent>)klass.newInstance();
      } catch (Exception e) {
         LOGGER.debug("Invalid {} value: error creating {}: ", Log4jPropertyKey.ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME, cls, e);
         return new AsyncLoggerDefaultExceptionHandler();
      }
   }

   static ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper> getAsyncLoggerConfigExceptionHandler() {
      String cls = PropertiesUtil.getProperties().getStringProperty(Log4jPropertyKey.ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME);
      if (cls == null) {
         return new AsyncLoggerConfigDefaultExceptionHandler();
      }

      try {
         Class<? extends ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper>> klass = (Class<? extends ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper>>)Loader.loadClass(
            cls
         );
         return (ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper>)klass.newInstance();
      } catch (Exception e) {
         LOGGER.debug("Invalid {} value: error creating {}: ", Log4jPropertyKey.ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME, cls, e);
         return new AsyncLoggerConfigDefaultExceptionHandler();
      }
   }

   public static long getExecutorThreadId(final ExecutorService executor) {
      Future<Long> result = executor.submit(() -> Thread.currentThread().getId());

      try {
         return result.get();
      } catch (Exception ex) {
         String msg = "Could not obtain executor thread Id. Giving up to avoid the risk of application deadlock.";
         throw new IllegalStateException("Could not obtain executor thread Id. Giving up to avoid the risk of application deadlock.", ex);
      }
   }
}
