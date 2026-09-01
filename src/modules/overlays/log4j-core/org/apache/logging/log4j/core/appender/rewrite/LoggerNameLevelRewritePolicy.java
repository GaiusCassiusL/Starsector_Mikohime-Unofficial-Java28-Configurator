package org.apache.logging.log4j.core.appender.rewrite;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "rewritePolicy", printObject = true)
@Plugin("LoggerNameLevelRewritePolicy")
public final class LoggerNameLevelRewritePolicy implements RewritePolicy {
   private final String loggerName;
   private final Map<Level, Level> map;

   @PluginFactory
   public static LoggerNameLevelRewritePolicy createPolicy(
      @PluginAttribute("logger") final String loggerNamePrefix, @PluginElement("KeyValuePair") final KeyValuePair[] levelPairs
   ) {
      Map<Level, Level> newMap = new HashMap<>(levelPairs.length);

      for (KeyValuePair keyValuePair : levelPairs) {
         newMap.put(getLevel(keyValuePair.getKey()), getLevel(keyValuePair.getValue()));
      }

      return new LoggerNameLevelRewritePolicy(loggerNamePrefix, newMap);
   }

   private static Level getLevel(final String name) {
      return Level.getLevel(name.toUpperCase(Locale.ROOT));
   }

   private LoggerNameLevelRewritePolicy(final String loggerName, final Map<Level, Level> map) {
      this.loggerName = loggerName;
      this.map = map;
   }

   @Override
   public LogEvent rewrite(final LogEvent event) {
      if (event.getLoggerName() != null && event.getLoggerName().startsWith(this.loggerName)) {
         Level sourceLevel = event.getLevel();
         Level newLevel = this.map.get(sourceLevel);
         return newLevel != null && newLevel != sourceLevel ? new Log4jLogEvent.Builder(event).setLevel(newLevel).build() : event;
      } else {
         return event;
      }
   }
}
