package org.apache.logging.log4j.core.filter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "filter", printObject = true)
@Plugin
public final class BurstFilter extends AbstractFilter {
   private static final long NANOS_IN_SECONDS = 1000000000L;
   private static final int DEFAULT_RATE = 10;
   private static final int DEFAULT_RATE_MULTIPLE = 100;
   private static final int HASH_SHIFT = 32;
   private final Level level;
   private final long burstInterval;
   private final DelayQueue<BurstFilter.LogDelay> history = new DelayQueue<>();
   private final Queue<BurstFilter.LogDelay> available = new ConcurrentLinkedQueue<>();

   static BurstFilter.LogDelay createLogDelay(final long expireTime) {
      return new BurstFilter.LogDelay(expireTime);
   }

   private BurstFilter(final Level level, final float rate, final long maxBurst, final Filter.Result onMatch, final Filter.Result onMismatch) {
      super(onMatch, onMismatch);
      this.level = level;
      this.burstInterval = (long)(1.0E9F * ((float)maxBurst / rate));

      for (int i = 0; i < maxBurst; i++) {
         this.available.add(createLogDelay(0L));
      }
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final LogEvent event) {
      return this.filter(event.getLevel());
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(
      final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2, final Object p3
   ) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4
   ) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5
   ) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6
   ) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7
   ) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7,
      final Object p8
   ) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7,
      final Object p8,
      final Object p9
   ) {
      return this.filter(level);
   }

   private Filter.Result filter(final Level level) {
      if (!this.level.isMoreSpecificThan(level)) {
         return this.onMatch;
      }

      for (BurstFilter.LogDelay delay = this.history.poll(); delay != null; delay = this.history.poll()) {
         this.available.add(delay);
      }

      BurstFilter.LogDelay var3 = this.available.poll();
      if (var3 != null) {
         var3.setDelay(this.burstInterval);
         this.history.add(var3);
         return this.onMatch;
      } else {
         return this.onMismatch;
      }
   }

   public int getAvailable() {
      return this.available.size();
   }

   public void clear() {
      for (BurstFilter.LogDelay delay : this.history) {
         this.history.remove(delay);
         this.available.add(delay);
      }
   }

   @Override
   public String toString() {
      return "level=" + this.level.toString() + ", interval=" + this.burstInterval + ", max=" + this.history.size();
   }

   @PluginFactory
   public static BurstFilter.Builder newBuilder() {
      return new BurstFilter.Builder();
   }

   public static class Builder
      extends AbstractFilter.AbstractFilterBuilder<BurstFilter.Builder>
      implements org.apache.logging.log4j.plugins.util.Builder<BurstFilter> {
      @PluginBuilderAttribute
      private Level level = Level.WARN;
      @PluginBuilderAttribute
      private float rate = 10.0F;
      @PluginBuilderAttribute
      private long maxBurst;

      public BurstFilter.Builder setLevel(final Level level) {
         this.level = level;
         return this;
      }

      public BurstFilter.Builder setRate(final float rate) {
         this.rate = rate;
         return this;
      }

      public BurstFilter.Builder setMaxBurst(final long maxBurst) {
         this.maxBurst = maxBurst;
         return this;
      }

      public BurstFilter build() {
         if (this.rate <= 0.0F) {
            this.rate = 10.0F;
         }

         if (this.maxBurst <= 0L) {
            this.maxBurst = (long)(this.rate * 100.0F);
         }

         return new BurstFilter(this.level, this.rate, this.maxBurst, this.getOnMatch(), this.getOnMismatch());
      }
   }

   private static class LogDelay implements Delayed {
      private long expireTime;

      LogDelay(final long expireTime) {
         this.expireTime = expireTime;
      }

      public void setDelay(final long delay) {
         this.expireTime = delay + System.nanoTime();
      }

      @Override
      public long getDelay(final TimeUnit timeUnit) {
         return timeUnit.convert(this.expireTime - System.nanoTime(), TimeUnit.NANOSECONDS);
      }

      public int compareTo(final Delayed delayed) {
         long diff = this.expireTime - ((BurstFilter.LogDelay)delayed).expireTime;
         return Long.signum(diff);
      }

      @Override
      public boolean equals(final Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            BurstFilter.LogDelay logDelay = (BurstFilter.LogDelay)o;
            return this.expireTime == logDelay.expireTime;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return (int)(this.expireTime ^ this.expireTime >>> 32);
      }
   }
}
