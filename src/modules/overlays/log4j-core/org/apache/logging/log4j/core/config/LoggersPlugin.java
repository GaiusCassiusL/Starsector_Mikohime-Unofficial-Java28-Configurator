package org.apache.logging.log4j.core.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable
@Plugin("loggers")
public final class LoggersPlugin {
   private LoggersPlugin() {
   }

   @PluginFactory
   public static Loggers createLoggers(@PluginElement("Loggers") final LoggerConfig[] loggers) {
      ConcurrentMap<String, LoggerConfig> loggerMap = new ConcurrentHashMap<>();
      LoggerConfig root = null;

      for (LoggerConfig logger : loggers) {
         if (logger != null) {
            if (logger.getName().isEmpty()) {
               if (root != null) {
                  throw new IllegalStateException("Configuration has multiple root loggers. There can be only one.");
               }

               root = logger;
            }

            loggerMap.put(logger.getName(), logger);
         }
      }

      return new Loggers(loggerMap, root);
   }
}
