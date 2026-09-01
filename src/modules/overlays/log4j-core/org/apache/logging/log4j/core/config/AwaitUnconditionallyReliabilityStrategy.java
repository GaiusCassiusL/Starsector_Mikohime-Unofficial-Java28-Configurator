package org.apache.logging.log4j.core.config;

import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Supplier;

public class AwaitUnconditionallyReliabilityStrategy implements ReliabilityStrategy {
   private static final long DEFAULT_SLEEP_MILLIS = 5000L;
   private static final long SLEEP_MILLIS = sleepMillis();
   private final LoggerConfig loggerConfig;

   public AwaitUnconditionallyReliabilityStrategy(final LoggerConfig loggerConfig) {
      this.loggerConfig = Objects.requireNonNull(loggerConfig, "loggerConfig is null");
   }

   private static long sleepMillis() {
      return PropertiesUtil.getProperties().getLongProperty(Log4jPropertyKey.CONFIG_RELIABILITY_STRATEGY_AWAIT_UNCONDITIONALLY_MILLIS, 5000L);
   }

   @Override
   public void log(
      final Supplier<LoggerConfig> reconfigured,
      final String loggerName,
      final String fqcn,
      final Marker marker,
      final Level level,
      final Message data,
      final Throwable t
   ) {
      this.loggerConfig.log(loggerName, fqcn, marker, level, data, t);
   }

   @Override
   public void log(
      final Supplier<LoggerConfig> reconfigured,
      final String loggerName,
      final String fqcn,
      final StackTraceElement location,
      final Marker marker,
      final Level level,
      final Message data,
      final Throwable t
   ) {
      this.loggerConfig.log(loggerName, fqcn, location, marker, level, data, t);
   }

   @Override
   public void log(final Supplier<LoggerConfig> reconfigured, final LogEvent event) {
      this.loggerConfig.log(event);
   }

   @Override
   public LoggerConfig getActiveLoggerConfig(final Supplier<LoggerConfig> next) {
      return this.loggerConfig;
   }

   @Override
   public void afterLogEvent() {
   }

   @Override
   public void beforeStopAppenders() {
   }

   @Override
   public void beforeStopConfiguration(final Configuration configuration) {
      if (this.loggerConfig == configuration.getRootLogger()) {
         try {
            Thread.sleep(SLEEP_MILLIS);
         } catch (InterruptedException e) {
            StatusLogger.getLogger().warn("Sleep before stop configuration was interrupted.");
         }
      }
   }
}
