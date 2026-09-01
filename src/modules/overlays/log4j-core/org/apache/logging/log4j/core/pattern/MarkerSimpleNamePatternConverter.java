package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("MarkerNamePatternConverter")
@ConverterKeys("markerSimpleName")
@PerformanceSensitive("allocation")
public final class MarkerSimpleNamePatternConverter extends LogEventPatternConverter {
   private MarkerSimpleNamePatternConverter(final String[] options) {
      super("MarkerSimpleName", "markerSimpleName");
   }

   public static MarkerSimpleNamePatternConverter newInstance(final String[] options) {
      return new MarkerSimpleNamePatternConverter(options);
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      Marker marker = event.getMarker();
      if (marker != null) {
         toAppendTo.append(marker.getName());
      }
   }
}
