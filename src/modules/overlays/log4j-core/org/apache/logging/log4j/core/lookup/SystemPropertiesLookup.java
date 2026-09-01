package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

@Lookup
@Plugin("sys")
public class SystemPropertiesLookup extends AbstractLookup {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final Marker LOOKUP = MarkerManager.getMarker("LOOKUP");

   @Override
   public String lookup(final LogEvent event, final String key) {
      try {
         return System.getProperty(key);
      } catch (Exception ex) {
         LOGGER.warn(LOOKUP, "Error while getting system property [{}].", key, ex);
         return null;
      }
   }
}
