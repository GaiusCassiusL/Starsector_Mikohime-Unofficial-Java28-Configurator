package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Plugin;

@Lookup
@Plugin("marker")
public class MarkerLookup extends AbstractLookup {
   static final String MARKER = "marker";

   @Override
   public String lookup(final LogEvent event, final String key) {
      Marker marker = event == null ? null : event.getMarker();
      return marker == null ? null : marker.getName();
   }

   @Override
   public String lookup(final String key) {
      return MarkerManager.exists(key) ? key : null;
   }
}
