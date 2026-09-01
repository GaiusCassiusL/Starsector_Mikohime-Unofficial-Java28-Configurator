package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable
@Plugin("failovers")
public final class FailoversPlugin {
   private static final Logger LOGGER = StatusLogger.getLogger();

   private FailoversPlugin() {
   }

   @PluginFactory
   public static String[] createFailovers(@PluginElement("AppenderRef") final AppenderRef... refs) {
      if (refs == null) {
         LOGGER.error("failovers must contain an appender reference");
         return null;
      }

      String[] arr = new String[refs.length];

      for (int i = 0; i < refs.length; i++) {
         arr[i] = refs[i].getRef();
      }

      return arr;
   }
}
