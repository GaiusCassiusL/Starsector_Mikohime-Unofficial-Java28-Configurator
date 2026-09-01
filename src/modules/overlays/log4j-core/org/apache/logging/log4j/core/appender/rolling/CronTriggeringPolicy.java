package org.apache.logging.log4j.core.appender.rolling;

import java.text.ParseException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.CronScheduledFuture;
import org.apache.logging.log4j.core.config.Scheduled;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin
@Scheduled
public final class CronTriggeringPolicy extends AbstractTriggeringPolicy {
   private static final String defaultSchedule = "0 0 0 * * ?";
   private RollingFileManager manager;
   private final CronExpression cronExpression;
   private final Configuration configuration;
   private final boolean checkOnStartup;
   private volatile Date lastRollDate;
   private CronScheduledFuture<?> future;

   private CronTriggeringPolicy(final CronExpression schedule, final boolean checkOnStartup, final Configuration configuration) {
      this.cronExpression = Objects.requireNonNull(schedule, "schedule");
      this.configuration = Objects.requireNonNull(configuration, "configuration");
      this.checkOnStartup = checkOnStartup;
   }

   @Override
   public void initialize(final RollingFileManager aManager) {
      this.manager = aManager;
      Date now = new Date();
      Date lastRollForFile = this.cronExpression.getPrevFireTime(new Date(this.manager.getFileTime()));
      Date lastRegularRoll = this.cronExpression.getPrevFireTime(new Date());
      aManager.getPatternProcessor().setCurrentFileTime(lastRegularRoll.getTime());
      LOGGER.debug("LastRollForFile {}, LastRegularRole {}", lastRollForFile, lastRegularRoll);
      aManager.getPatternProcessor().setPrevFileTime(lastRegularRoll.getTime());
      aManager.getPatternProcessor().setTimeBased(true);
      if (this.checkOnStartup && lastRollForFile != null && lastRegularRoll != null && lastRollForFile.before(lastRegularRoll)) {
         this.lastRollDate = lastRollForFile;
         this.rollover();
      }

      ConfigurationScheduler scheduler = this.configuration.getScheduler();
      if (!scheduler.isExecutorServiceSet()) {
         scheduler.incrementScheduledItems();
      }

      if (!scheduler.isStarted()) {
         scheduler.start();
      }

      this.lastRollDate = lastRegularRoll;
      this.future = scheduler.scheduleWithCron(this.cronExpression, now, new CronTriggeringPolicy.CronTrigger());
      LOGGER.debug(scheduler.toString());
   }

   @Override
   public boolean isTriggeringEvent(final LogEvent event) {
      return false;
   }

   public CronExpression getCronExpression() {
      return this.cronExpression;
   }

   @PluginFactory
   public static CronTriggeringPolicy createPolicy(
      @PluginConfiguration final Configuration configuration, @PluginAttribute final String evaluateOnStartup, @PluginAttribute final String schedule
   ) {
      boolean checkOnStartup = Boolean.parseBoolean(evaluateOnStartup);
      CronExpression cronExpression;
      if (schedule == null) {
         LOGGER.info("No schedule specified, defaulting to Daily");
         cronExpression = getSchedule("0 0 0 * * ?");
      } else {
         cronExpression = getSchedule(schedule);
         if (cronExpression == null) {
            LOGGER.error("Invalid expression specified. Defaulting to Daily");
            cronExpression = getSchedule("0 0 0 * * ?");
         }
      }

      return new CronTriggeringPolicy(cronExpression, checkOnStartup, configuration);
   }

   private static CronExpression getSchedule(final String expression) {
      try {
         return new CronExpression(expression);
      } catch (ParseException pe) {
         LOGGER.error("Invalid cron expression - " + expression, pe);
         return null;
      }
   }

   private void rollover() {
      Date rollTime = this.future != null ? this.future.getFireTime() : new Date();
      this.manager.rollover(this.cronExpression.getPrevFireTime(rollTime).getTime(), this.lastRollDate.getTime());
      if (this.future != null) {
         this.lastRollDate = this.future.getFireTime();
      }
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      this.setStopping();
      boolean stopped = this.stop(this.future);
      this.setStopped();
      return stopped;
   }

   @Override
   public String toString() {
      return "CronTriggeringPolicy(schedule=" + this.cronExpression.getCronExpression() + ")";
   }

   private class CronTrigger implements Runnable {
      @Override
      public void run() {
         CronTriggeringPolicy.this.rollover();
      }
   }
}
