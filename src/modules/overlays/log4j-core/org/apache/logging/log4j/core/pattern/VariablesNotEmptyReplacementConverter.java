package org.apache.logging.log4j.core.pattern;

import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("notEmpty")
@ConverterKeys({"notEmpty", "varsNotEmpty", "variablesNotEmpty"})
@PerformanceSensitive("allocation")
public final class VariablesNotEmptyReplacementConverter extends LogEventPatternConverter {
   private final List<PatternFormatter> formatters;

   private VariablesNotEmptyReplacementConverter(final List<PatternFormatter> formatters) {
      super("notEmpty", "notEmpty");
      this.formatters = formatters;
   }

   public static VariablesNotEmptyReplacementConverter newInstance(final Configuration config, final String[] options) {
      if (options.length != 1) {
         LOGGER.error("Incorrect number of options on varsNotEmpty. Expected 1 received " + options.length);
         return null;
      } else if (options[0] == null) {
         LOGGER.error("No pattern supplied on varsNotEmpty");
         return null;
      } else {
         PatternParser parser = PatternLayout.createPatternParser(config);
         List<PatternFormatter> formatters = parser.parse(options[0]);
         return new VariablesNotEmptyReplacementConverter(formatters);
      }
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      int start = toAppendTo.length();
      boolean allVarsEmpty = true;
      boolean hasVars = false;

      for (int i = 0; i < this.formatters.size(); i++) {
         PatternFormatter formatter = this.formatters.get(i);
         int formatterStart = toAppendTo.length();
         formatter.format(event, toAppendTo);
         LogEventPatternConverter converter = formatter.getConverter();
         if (converter.isVariable()) {
            hasVars = true;
            allVarsEmpty = allVarsEmpty && sequenceRegionMatches(toAppendTo, formatterStart, converter.emptyVariableOutput());
         }
      }

      if (!hasVars || allVarsEmpty) {
         toAppendTo.setLength(start);
      }
   }

   private static boolean sequenceRegionMatches(final CharSequence sequence1, final int sequence1Offset, final CharSequence sequence2) {
      boolean lengthMatches = sequence1.length() - sequence1Offset == sequence2.length();
      if (!lengthMatches) {
         return false;
      }

      for (int i2 = 0; i2 < sequence2.length(); i2++) {
         char c2 = sequence2.charAt(i2);
         int i1 = i2 + sequence1Offset;
         char c1 = sequence1.charAt(i1);
         if (c2 != c1) {
            return false;
         }
      }

      return true;
   }
}
