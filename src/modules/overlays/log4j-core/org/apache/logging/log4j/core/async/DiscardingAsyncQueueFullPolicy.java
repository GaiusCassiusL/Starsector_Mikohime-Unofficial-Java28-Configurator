package org.apache.logging.log4j.core.async;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public class DiscardingAsyncQueueFullPolicy extends DefaultAsyncQueueFullPolicy {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final Level thresholdLevel;
   private final AtomicLong discardCount = new AtomicLong();

   public DiscardingAsyncQueueFullPolicy(final Level thresholdLevel) {
      this.thresholdLevel = Objects.requireNonNull(thresholdLevel, "thresholdLevel");
   }

   @Override
   public EventRoute getRoute(final long backgroundThreadId, final Level level) {
      if (level.isLessSpecificThan(this.thresholdLevel)) {
         if (this.discardCount.getAndIncrement() == 0L) {
            LOGGER.warn(
               "Async queue is full, discarding event with level {}. This message will only appear once; future events from {} are silently discarded until queue capacity becomes available.",
               level,
               this.thresholdLevel
            );
         }

         return EventRoute.DISCARD;
      } else {
         return super.getRoute(backgroundThreadId, level);
      }
   }

   public static long getDiscardCount(final AsyncQueueFullPolicy router) {
      return router instanceof DiscardingAsyncQueueFullPolicy ? ((DiscardingAsyncQueueFullPolicy)router).discardCount.get() : 0L;
   }

   public Level getThresholdLevel() {
      return this.thresholdLevel;
   }
}
