package org.apache.logging.log4j.core.pattern;

import java.util.List;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StringBuilders;

@Namespace("Converter")
@Plugin("equals")
@ConverterKeys("equals")
@PerformanceSensitive("allocation")
public final class EqualsReplacementConverter extends EqualsBaseReplacementConverter {
   public static EqualsReplacementConverter newInstance(final Configuration config, final String[] options) {
      if (options.length != 3) {
         LOGGER.error("Incorrect number of options on equals. Expected 3 received " + options.length);
         return null;
      } else if (options[0] == null) {
         LOGGER.error("No pattern supplied on equals");
         return null;
      } else if (options[1] == null) {
         LOGGER.error("No test string supplied on equals");
         return null;
      } else if (options[2] == null) {
         LOGGER.error("No substitution supplied on equals");
         return null;
      } else {
         String p = options[1];
         PatternParser parser = PatternLayout.createPatternParser(config);
         List<PatternFormatter> formatters = parser.parse(options[0]);
         return new EqualsReplacementConverter(formatters, p, options[2], parser);
      }
   }

   private EqualsReplacementConverter(final List<PatternFormatter> formatters, final String testString, final String substitution, final PatternParser parser) {
      super("equals", "equals", formatters, testString, substitution, parser);
   }

   @Override
   protected boolean equals(final String str, final StringBuilder buff, final int from, final int len) {
      return StringBuilders.equals(str, 0, str.length(), buff, from, len);
   }
}
