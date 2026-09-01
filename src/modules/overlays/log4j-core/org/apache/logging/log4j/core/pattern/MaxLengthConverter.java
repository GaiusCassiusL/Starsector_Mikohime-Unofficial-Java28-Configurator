package org.apache.logging.log4j.core.pattern;

import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("maxLength")
@ConverterKeys({"maxLength", "maxLen"})
@PerformanceSensitive("allocation")
public final class MaxLengthConverter extends LogEventPatternConverter {
   private final List<PatternFormatter> formatters;
   private final int maxLength;

   public static MaxLengthConverter newInstance(final Configuration config, final String[] options) {
      if (options.length != 2) {
         LOGGER.error("Incorrect number of options on maxLength: expected 2 received {}: {}", options.length, options);
         return null;
      } else if (options[0] == null) {
         LOGGER.error("No pattern supplied on maxLength");
         return null;
      } else if (options[1] == null) {
         LOGGER.error("No length supplied on maxLength");
         return null;
      } else {
         PatternParser parser = PatternLayout.createPatternParser(config);
         List<PatternFormatter> formatters = parser.parse(options[0]);
         return new MaxLengthConverter(formatters, AbstractAppender.parseInt(options[1], 100));
      }
   }

   private MaxLengthConverter(final List<PatternFormatter> formatters, final int maxLength) {
      super("MaxLength", "maxLength");
      this.maxLength = maxLength;
      this.formatters = formatters;
      LOGGER.trace("new MaxLengthConverter with {}", maxLength);
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      int initialLength = toAppendTo.length();

      for (int i = 0; i < this.formatters.size(); i++) {
         PatternFormatter formatter = this.formatters.get(i);
         formatter.format(event, toAppendTo);
         if (toAppendTo.length() > initialLength + this.maxLength) {
            break;
         }
      }

      if (toAppendTo.length() > initialLength + this.maxLength) {
         toAppendTo.setLength(initialLength + this.maxLength);
         if (this.maxLength > 20) {
            toAppendTo.append("...");
         }
      }
   }
}
