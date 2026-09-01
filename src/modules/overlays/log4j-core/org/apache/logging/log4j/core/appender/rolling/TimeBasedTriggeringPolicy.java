package org.apache.logging.log4j.core.appender.rolling;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin
public final class TimeBasedTriggeringPolicy extends AbstractTriggeringPolicy {
   private long nextRolloverMillis;
   private final int interval;
   private final boolean modulate;
   private final long maxRandomDelayMillis;
   private final Clock clock;
   private RollingFileManager manager;

   private TimeBasedTriggeringPolicy(final int interval, final boolean modulate, final long maxRandomDelayMillis, final Clock clock) {
      this.interval = interval;
      this.modulate = modulate;
      this.maxRandomDelayMillis = maxRandomDelayMillis;
      this.clock = clock;
   }

   public int getInterval() {
      return this.interval;
   }

   public long getNextRolloverMillis() {
      return this.nextRolloverMillis;
   }

   @Override
   public void initialize(final RollingFileManager aManager) {
      this.manager = aManager;
      long current = aManager.getFileTime();
      if (current == 0L) {
         current = this.clock.currentTimeMillis();
      }

      aManager.getPatternProcessor().getNextTime(current, this.interval, this.modulate);
      aManager.getPatternProcessor().setTimeBased(true);
      this.nextRolloverMillis = ThreadLocalRandom.current().nextLong(0L, 1L + this.maxRandomDelayMillis)
         + aManager.getPatternProcessor().getNextTime(current, this.interval, this.modulate);
   }

   @Override
   public boolean isTriggeringEvent(final LogEvent event) {
      long nowMillis = event.getTimeMillis();
      if (nowMillis >= this.nextRolloverMillis) {
         this.nextRolloverMillis = ThreadLocalRandom.current().nextLong(0L, 1L + this.maxRandomDelayMillis)
            + this.manager.getPatternProcessor().getNextTime(nowMillis, this.interval, this.modulate);
         this.manager.getPatternProcessor().setCurrentFileTime(this.clock.currentTimeMillis());
         return true;
      } else {
         return false;
      }
   }

   @PluginFactory
   public static TimeBasedTriggeringPolicy.Builder newBuilder() {
      return new TimeBasedTriggeringPolicy.Builder();
   }

   @Override
   public String toString() {
      return "TimeBasedTriggeringPolicy(nextRolloverMillis=" + this.nextRolloverMillis + ", interval=" + this.interval + ", modulate=" + this.modulate + ")";
   }

   public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<TimeBasedTriggeringPolicy> {
      @PluginBuilderAttribute
      private int interval = 1;
      @PluginBuilderAttribute
      private boolean modulate = false;
      @PluginBuilderAttribute
      private int maxRandomDelay = 0;
      private Clock clock;

      public TimeBasedTriggeringPolicy build() {
         long maxRandomDelayMillis = TimeUnit.SECONDS.toMillis(this.maxRandomDelay);
         return new TimeBasedTriggeringPolicy(this.interval, this.modulate, maxRandomDelayMillis, this.clock);
      }

      public int getInterval() {
         return this.interval;
      }

      public boolean isModulate() {
         return this.modulate;
      }

      public int getMaxRandomDelay() {
         return this.maxRandomDelay;
      }

      public Clock getClock() {
         return this.clock;
      }

      public TimeBasedTriggeringPolicy.Builder setInterval(final int interval) {
         this.interval = interval;
         return this;
      }

      public TimeBasedTriggeringPolicy.Builder setModulate(final boolean modulate) {
         this.modulate = modulate;
         return this;
      }

      public TimeBasedTriggeringPolicy.Builder setMaxRandomDelay(final int maxRandomDelay) {
         this.maxRandomDelay = maxRandomDelay;
         return this;
      }

      @Inject
      public TimeBasedTriggeringPolicy.Builder setClock(final Clock clock) {
         this.clock = clock;
         return this;
      }
   }
}
