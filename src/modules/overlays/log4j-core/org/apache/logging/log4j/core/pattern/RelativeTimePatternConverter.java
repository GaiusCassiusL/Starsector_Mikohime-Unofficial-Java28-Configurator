package org.apache.logging.log4j.core.pattern;

import java.lang.management.ManagementFactory;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("RelativeTimePatternConverter")
@ConverterKeys({"r", "relative"})
@PerformanceSensitive("allocation")
public class RelativeTimePatternConverter extends LogEventPatternConverter {
   private final long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();

   public RelativeTimePatternConverter() {
      super("Time", "time");
   }

   public static RelativeTimePatternConverter newInstance(final String[] options) {
      return new RelativeTimePatternConverter();
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      long timestamp = event.getTimeMillis();
      toAppendTo.append(timestamp - this.startTime);
   }
}
