package org.apache.logging.log4j.core.appender.routing;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.Scheduled;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin
@Scheduled
public class IdlePurgePolicy extends AbstractLifeCycle implements PurgePolicy, Runnable {
   private final long timeToLive;
   private final long checkInterval;
   private final ConcurrentMap<String, Long> appendersUsage = new ConcurrentHashMap<>();
   private RoutingAppender routingAppender;
   private final ConfigurationScheduler scheduler;
   private volatile ScheduledFuture<?> future;

   public IdlePurgePolicy(final long timeToLive, final long checkInterval, final ConfigurationScheduler scheduler) {
      this.timeToLive = timeToLive;
      this.checkInterval = checkInterval;
      this.scheduler = scheduler;
   }

   @Override
   public void initialize(final RoutingAppender routingAppender) {
      this.routingAppender = routingAppender;
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      this.setStopping();
      boolean stopped = this.stop(this.future);
      this.setStopped();
      return stopped;
   }

   @Override
   public void purge() {
      long createTime = System.currentTimeMillis() - this.timeToLive;

      for (Entry<String, Long> entry : this.appendersUsage.entrySet()) {
         long entryValue = entry.getValue();
         if (entryValue < createTime && this.appendersUsage.remove(entry.getKey(), entryValue)) {
            LOGGER.debug("Removing appender {}", entry.getKey());
            this.routingAppender.deleteAppender(entry.getKey());
         }
      }
   }

   @Override
   public void update(final String key, final LogEvent event) {
      long now = System.currentTimeMillis();
      this.appendersUsage.put(key, now);
      if (this.future == null) {
         synchronized (this) {
            if (this.future == null) {
               this.scheduleNext();
            }
         }
      }
   }

   @Override
   public void run() {
      this.purge();
      this.scheduleNext();
   }

   private void scheduleNext() {
      long updateTime = Long.MAX_VALUE;

      for (Entry<String, Long> entry : this.appendersUsage.entrySet()) {
         if (entry.getValue() < updateTime) {
            updateTime = entry.getValue();
         }
      }

      if (updateTime < Long.MAX_VALUE) {
         long interval = this.timeToLive - (System.currentTimeMillis() - updateTime);
         this.future = this.scheduler.schedule(this, interval, TimeUnit.MILLISECONDS);
      } else {
         this.future = this.scheduler.schedule(this, this.checkInterval, TimeUnit.MILLISECONDS);
      }
   }

   @PluginFactory
   public static PurgePolicy createPurgePolicy(
      @PluginAttribute final String timeToLive,
      @PluginAttribute final String checkInterval,
      @PluginAttribute final String timeUnit,
      @PluginConfiguration final Configuration configuration
   ) {
      if (timeToLive == null) {
         LOGGER.error("A timeToLive value is required");
         return null;
      }

      TimeUnit units;
      if (timeUnit == null) {
         units = TimeUnit.MINUTES;
      } else {
         try {
            units = TimeUnit.valueOf(timeUnit.toUpperCase());
         } catch (Exception ex) {
            LOGGER.error("Invalid timeUnit value {}. timeUnit set to MINUTES", timeUnit, ex);
            units = TimeUnit.MINUTES;
         }
      }

      long ttl = units.toMillis(Long.parseLong(timeToLive));
      if (ttl < 0L) {
         LOGGER.error("timeToLive must be positive. timeToLive set to 0");
         ttl = 0L;
      }

      long ci;
      if (checkInterval == null) {
         ci = ttl;
      } else {
         ci = units.toMillis(Long.parseLong(checkInterval));
         if (ci < 0L) {
            LOGGER.error("checkInterval must be positive. checkInterval set equal to timeToLive = {}", ttl);
            ci = ttl;
         }
      }

      return new IdlePurgePolicy(ttl, ci, configuration.getScheduler());
   }

   @Override
   public String toString() {
      return "timeToLive=" + this.timeToLive;
   }
}
