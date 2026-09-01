package org.apache.logging.log4j.core.pattern;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("highlight")
@ConverterKeys("highlight")
@PerformanceSensitive("allocation")
public final class HighlightConverter extends LogEventPatternConverter implements AnsiConverter {
   private static final Map<String, String> DEFAULT_STYLES = Map.of(
      Level.FATAL.name(),
      AnsiEscape.createSequence("BRIGHT", "RED"),
      Level.ERROR.name(),
      AnsiEscape.createSequence("BRIGHT", "RED"),
      Level.WARN.name(),
      AnsiEscape.createSequence("YELLOW"),
      Level.INFO.name(),
      AnsiEscape.createSequence("GREEN"),
      Level.DEBUG.name(),
      AnsiEscape.createSequence("CYAN"),
      Level.TRACE.name(),
      AnsiEscape.createSequence("BLACK")
   );
   private static final Map<String, String> LOGBACK_STYLES = Map.of(
      Level.FATAL.name(),
      AnsiEscape.createSequence("BLINK", "BRIGHT", "RED"),
      Level.ERROR.name(),
      AnsiEscape.createSequence("BRIGHT", "RED"),
      Level.WARN.name(),
      AnsiEscape.createSequence("RED"),
      Level.INFO.name(),
      AnsiEscape.createSequence("BLUE"),
      Level.DEBUG.name(),
      AnsiEscape.createSequence((String[])null),
      Level.TRACE.name(),
      AnsiEscape.createSequence((String[])null)
   );
   private static final String STYLE_KEY = "STYLE";
   private static final String STYLE_KEY_DEFAULT = "DEFAULT";
   private static final String STYLE_KEY_LOGBACK = "LOGBACK";
   private static final Map<String, Map<String, String>> STYLES = Map.of("DEFAULT", DEFAULT_STYLES, "LOGBACK", LOGBACK_STYLES);
   private final Map<String, String> levelStyles;
   private final List<PatternFormatter> patternFormatters;
   private final boolean noAnsi;
   private final String defaultStyle;

   private static Map<String, String> createLevelStyleMap(final String[] options) {
      if (options.length < 2) {
         return DEFAULT_STYLES;
      }

      String string = options[1].replaceAll("disableAnsi=(true|false)", "").replaceAll("noConsoleNoAnsi=(true|false)", "");
      Map<String, String> styles = AnsiEscape.createMap(string, new String[]{"STYLE"});
      Map<String, String> levelStyles = new HashMap<>(DEFAULT_STYLES);

      for (Entry<String, String> entry : styles.entrySet()) {
         String key = entry.getKey().toUpperCase(Locale.ENGLISH);
         String value = entry.getValue();
         if ("STYLE".equalsIgnoreCase(key)) {
            Map<String, String> enumMap = STYLES.get(value.toUpperCase(Locale.ENGLISH));
            if (enumMap == null) {
               LOGGER.error("Unknown level style: " + value + ". Use one of " + Arrays.toString(STYLES.keySet().toArray()));
            } else {
               levelStyles.putAll(enumMap);
            }
         } else {
            Level level = Level.toLevel(key, null);
            if (level == null) {
               LOGGER.warn("Setting style for yet unknown level name {}", key);
               levelStyles.put(key, value);
            } else {
               levelStyles.put(level.name(), value);
            }
         }
      }

      return levelStyles;
   }

   public static HighlightConverter newInstance(final Configuration config, final String[] options) {
      if (options.length < 1) {
         LOGGER.error("Incorrect number of options on style. Expected at least 1, received " + options.length);
         return null;
      } else if (options[0] == null) {
         LOGGER.error("No pattern supplied on style");
         return null;
      } else {
         PatternParser parser = PatternLayout.createPatternParser(config);
         List<PatternFormatter> formatters = parser.parse(options[0]);
         boolean disableAnsi = Arrays.toString(options).contains("disableAnsi=true");
         boolean noConsoleNoAnsi = Arrays.toString(options).contains("noConsoleNoAnsi=true");
         boolean hideAnsi = disableAnsi || noConsoleNoAnsi && System.console() == null;
         return new HighlightConverter(formatters, createLevelStyleMap(options), hideAnsi);
      }
   }

   private HighlightConverter(final List<PatternFormatter> patternFormatters, final Map<String, String> levelStyles, final boolean noAnsi) {
      super("style", "style");
      this.patternFormatters = patternFormatters;
      this.levelStyles = levelStyles;
      this.defaultStyle = AnsiEscape.getDefaultStyle();
      this.noAnsi = noAnsi;
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      int start = 0;
      int end = 0;
      String levelStyle = this.levelStyles.get(event.getLevel().name());
      if (!this.noAnsi) {
         start = toAppendTo.length();
         if (levelStyle != null) {
            toAppendTo.append(levelStyle);
         }

         end = toAppendTo.length();
      }

      int i = 0;

      for (int size = this.patternFormatters.size(); i < size; i++) {
         this.patternFormatters.get(i).format(event, toAppendTo);
      }

      boolean empty = toAppendTo.length() == end;
      if (!this.noAnsi) {
         if (empty) {
            toAppendTo.setLength(start);
         } else if (levelStyle != null) {
            toAppendTo.append(this.defaultStyle);
         }
      }
   }

   String getLevelStyle(final Level level) {
      return this.levelStyles.get(level.name());
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
}
