package org.apache.logging.log4j.core.pattern;

import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("style")
@ConverterKeys("style")
@PerformanceSensitive("allocation")
public final class StyleConverter extends LogEventPatternConverter implements AnsiConverter {
   private final List<PatternFormatter> patternFormatters;
   private final boolean noAnsi;
   private final String style;
   private final String defaultStyle;

   private StyleConverter(final List<PatternFormatter> patternFormatters, final String style, final boolean noAnsi) {
      super("style", "style");
      this.patternFormatters = patternFormatters;
      this.style = style;
      this.defaultStyle = AnsiEscape.getDefaultStyle();
      this.noAnsi = noAnsi;
   }

   public static StyleConverter newInstance(final Configuration config, final String[] options) {
      if (options == null) {
         return null;
      } else if (options.length < 2) {
         LOGGER.error("Incorrect number of options on style. Expected at least 1, received " + options.length);
         return null;
      } else if (options[0] == null) {
         LOGGER.error("No pattern supplied for style converter");
         return null;
      } else if (options[1] == null) {
         LOGGER.error("No style attributes supplied for style converter");
         return null;
      } else {
         PatternParser parser = PatternLayout.createPatternParser(config);
         List<PatternFormatter> formatters = parser.parse(options[0]);
         String style = AnsiEscape.createSequence(options[1].split(Patterns.COMMA_SPACE_SEPARATOR));
         boolean disableAnsi = Arrays.toString(options).contains("disableAnsi=true");
         boolean noConsoleNoAnsi = Arrays.toString(options).contains("noConsoleNoAnsi=true");
         boolean hideAnsi = disableAnsi || noConsoleNoAnsi && System.console() == null;
         return new StyleConverter(formatters, style, hideAnsi);
      }
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      int start = 0;
      int end = 0;
      if (!this.noAnsi) {
         start = toAppendTo.length();
         toAppendTo.append(this.style);
         end = toAppendTo.length();
      }

      int i = 0;

      for (int size = this.patternFormatters.size(); i < size; i++) {
         this.patternFormatters.get(i).format(event, toAppendTo);
      }

      if (!this.noAnsi) {
         if (toAppendTo.length() == end) {
            toAppendTo.setLength(start);
         } else {
            toAppendTo.append(this.defaultStyle);
         }
      }
   }

   @Override
   public boolean handlesThrowable() {
      for (PatternFormatter formatter : this.patternFormatters) {
         if (formatter.handlesThrowable()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("[style=");
      sb.append(this.style);
      sb.append(", defaultStyle=");
      sb.append(this.defaultStyle);
      sb.append(", patternFormatters=");
      sb.append(this.patternFormatters);
      sb.append(", noAnsi=");
      sb.append(this.noAnsi);
      sb.append(']');
      return sb.toString();
   }
}
