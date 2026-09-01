package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StringBuilders;

@Namespace("Converter")
@Plugin("MarkerPatternConverter")
@ConverterKeys("marker")
@PerformanceSensitive("allocation")
public final class MarkerPatternConverter extends LogEventPatternConverter {
   private MarkerPatternConverter(final String[] options) {
      super("Marker", "marker");
   }

   public static MarkerPatternConverter newInstance(final String[] options) {
      return new MarkerPatternConverter(options);
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      Marker marker = event.getMarker();
      if (marker != null) {
         StringBuilders.appendValue(toAppendTo, marker);
      }
   }
}
