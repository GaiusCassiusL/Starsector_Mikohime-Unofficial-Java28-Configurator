package org.apache.logging.log4j.core.pattern;

import java.util.List;
import java.util.regex.Pattern;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("Converter")
@Plugin("replace")
@ConverterKeys("replace")
public final class RegexReplacementConverter extends LogEventPatternConverter {
   private final Pattern pattern;
   private final String substitution;
   private final List<PatternFormatter> formatters;

   private RegexReplacementConverter(final List<PatternFormatter> formatters, final Pattern pattern, final String substitution) {
      super("replace", "replace");
      this.pattern = pattern;
      this.substitution = substitution;
      this.formatters = formatters;
   }

   public static RegexReplacementConverter newInstance(final Configuration config, final String[] options) {
      if (options.length != 3) {
         LOGGER.error("Incorrect number of options on replace. Expected 3 received " + options.length);
         return null;
      } else if (options[0] == null) {
         LOGGER.error("No pattern supplied on replace");
         return null;
      } else if (options[1] == null) {
         LOGGER.error("No regular expression supplied on replace");
         return null;
      } else if (options[2] == null) {
         LOGGER.error("No substitution supplied on replace");
         return null;
      } else {
         Pattern p = Pattern.compile(options[1]);
         PatternParser parser = PatternLayout.createPatternParser(config);
         List<PatternFormatter> formatters = parser.parse(options[0]);
         return new RegexReplacementConverter(formatters, p, options[2]);
      }
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      StringBuilder buf = new StringBuilder();

      for (PatternFormatter formatter : this.formatters) {
         formatter.format(event, buf);
      }

      toAppendTo.append(this.pattern.matcher(buf.toString()).replaceAll(this.substitution));
   }
}
