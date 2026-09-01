package org.apache.log4j.legacy.core;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.spi.LoggerContext;

public final class CategoryUtil {
   private static Logger asCore(final org.apache.logging.log4j.Logger logger) {
      return (Logger)logger;
   }

   private static <T> T get(final org.apache.logging.log4j.Logger logger, final Supplier<T> run, final T defaultValue) {
      return isCore(logger) ? run.get() : defaultValue;
   }

   public static Map<String, Appender> getAppenders(final org.apache.logging.log4j.Logger logger) {
      return get(logger, () -> getDirectAppenders(logger), Collections.emptyMap());
   }

   private static Map<String, Appender> getDirectAppenders(final org.apache.logging.log4j.Logger logger) {
      return getExactLoggerConfig(logger).<Map<String, Appender>>map(LoggerConfig::getAppenders).orElse(Collections.emptyMap());
   }

   private static Optional<LoggerConfig> getExactLoggerConfig(final org.apache.logging.log4j.Logger logger) {
      return Optional.of(asCore(logger).get()).filter(lc -> logger.getName().equals(lc.getName()));
   }

   public static Iterator<Filter> getFilters(final org.apache.logging.log4j.Logger logger) {
      return get(logger, asCore(logger)::getFilters, null);
   }

   public static LoggerContext getLoggerContext(final org.apache.logging.log4j.Logger logger) {
      return get(logger, asCore(logger)::getContext, null);
   }

   public static org.apache.logging.log4j.Logger getParent(final org.apache.logging.log4j.Logger logger) {
      return get(logger, asCore(logger)::getParent, null);
   }

   public static boolean isAdditive(final org.apache.logging.log4j.Logger logger) {
      return get(logger, asCore(logger)::isAdditive, false);
   }

   private static boolean isCore(final org.apache.logging.log4j.Logger logger) {
      return logger instanceof Logger;
   }

   public static void setAdditivity(final org.apache.logging.log4j.Logger logger, final boolean additive) {
      if (isCore(logger)) {
         asCore(logger).setAdditive(additive);
      }
   }

   public static void setLevel(final org.apache.logging.log4j.Logger logger, final Level level) {
      if (isCore(logger)) {
         asCore(logger).setLevel(level);
      }
   }

   public static void addAppender(final org.apache.logging.log4j.Logger logger, final Appender appender) {
      if (appender instanceof AppenderAdapter.Adapter) {
         appender.start();
      }

      asCore(logger).addAppender(appender);
   }

   public static void log(final org.apache.logging.log4j.Logger logger, final LogEvent event) {
      getExactLoggerConfig(logger).ifPresent(lc -> lc.log(event));
   }

   private CategoryUtil() {
   }
}
