package org.apache.log4j.helpers;

import org.apache.logging.log4j.status.StatusLogger;

public class LogLog {
   private static final StatusLogger LOGGER = StatusLogger.getLogger();
   public static final String DEBUG_KEY = "log4j.debug";
   @Deprecated
   public static final String CONFIG_DEBUG_KEY = "log4j.configDebug";
   protected static boolean debugEnabled = false;
   private static boolean quietMode = false;

   public static void debug(final String message) {
      if (debugEnabled && !quietMode) {
         LOGGER.debug(message);
      }
   }

   public static void debug(final String message, final Throwable throwable) {
      if (debugEnabled && !quietMode) {
         LOGGER.debug(message, throwable);
      }
   }

   public static void error(final String message) {
      if (!quietMode) {
         LOGGER.error(message);
      }
   }

   public static void error(final String message, final Throwable throwable) {
      if (!quietMode) {
         LOGGER.error(message, throwable);
      }
   }

   public static void setInternalDebugging(final boolean enabled) {
      debugEnabled = enabled;
   }

   public static void setQuietMode(final boolean quietMode) {
      LogLog.quietMode = quietMode;
   }

   public static void warn(final String message) {
      if (!quietMode) {
         LOGGER.warn(message);
      }
   }

   public static void warn(final String message, final Throwable throwable) {
      if (!quietMode) {
         LOGGER.warn(message, throwable);
      }
   }

   static {
      String key = OptionConverter.getSystemProperty("log4j.debug", null);
      if (key == null) {
         key = OptionConverter.getSystemProperty("log4j.configDebug", null);
      }

      if (key != null) {
         debugEnabled = OptionConverter.toBoolean(key, true);
      }
   }
}
