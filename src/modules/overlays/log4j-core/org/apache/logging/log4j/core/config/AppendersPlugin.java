package org.apache.logging.log4j.core.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable
@Plugin("appenders")
public final class AppendersPlugin {
   private AppendersPlugin() {
   }

   @PluginFactory
   public static ConcurrentMap<String, Appender> createAppenders(@PluginElement("Appenders") final Appender[] appenders) {
      ConcurrentMap<String, Appender> map = new ConcurrentHashMap<>(appenders.length);

      for (Appender appender : appenders) {
         map.put(appender.getName(), appender);
      }

      return map;
   }
}
