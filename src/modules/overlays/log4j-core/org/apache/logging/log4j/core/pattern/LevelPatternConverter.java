package org.apache.logging.log4j.core.pattern;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("LevelPatternConverter")
@ConverterKeys({"p", "level"})
@PerformanceSensitive("allocation")
public abstract class LevelPatternConverter extends LogEventPatternConverter {
   private static final String OPTION_LENGTH = "length";
   private static final String OPTION_LOWER = "lowerCase";
   private static final LevelPatternConverter INSTANCE = new LevelPatternConverter.SimpleLevelPatternConverter();

   private LevelPatternConverter() {
      super("Level", "level");
   }

   public static LevelPatternConverter newInstance(final String[] options) {
      if (options != null && options.length != 0) {
         Map<Level, String> levelMap = new HashMap<>();
         int length = Integer.MAX_VALUE;
         boolean lowerCase = false;
         String[] definitions = options[0].split(Patterns.COMMA_SEPARATOR);

         for (String def : definitions) {
            String[] pair = def.split("=");
            if (pair != null && pair.length == 2) {
               String key = pair[0].trim();
               String value = pair[1].trim();
               if ("length".equalsIgnoreCase(key)) {
                  length = Integer.parseInt(value);
               } else if ("lowerCase".equalsIgnoreCase(key)) {
                  lowerCase = Boolean.parseBoolean(value);
               } else {
                  Level level = Level.toLevel(key, null);
                  if (level == null) {
                     LOGGER.error("Invalid Level {}", key);
                  } else {
                     levelMap.put(level, value);
                  }
               }
            } else {
               LOGGER.error("Invalid option {}", def);
            }
         }

         if (levelMap.isEmpty() && length == Integer.MAX_VALUE && !lowerCase) {
            return INSTANCE;
         }

         for (Level level : Level.values()) {
            if (!levelMap.containsKey(level)) {
               String left = left(level, length);
               levelMap.put(level, lowerCase ? left.toLowerCase(Locale.US) : left);
            }
         }

         return new LevelPatternConverter.LevelMapLevelPatternConverter(levelMap);
      } else {
         return INSTANCE;
      }
   }

   private static String left(final Level level, final int length) {
      String string = level.toString();
      return length >= string.length() ? string : string.substring(0, length);
   }

   @Override
   public void format(final LogEvent event, final StringBuilder output) {
      throw new UnsupportedOperationException("Overridden by subclasses");
   }

   @Override
   public String getStyleClass(final Object e) {
      return e instanceof LogEvent ? "level " + ((LogEvent)e).getLevel().name().toLowerCase(Locale.ENGLISH) : "level";
   }

   private static final class LevelMapLevelPatternConverter extends LevelPatternConverter {
      private final Map<Level, String> levelMap;

      private LevelMapLevelPatternConverter(final Map<Level, String> levelMap) {
         this.levelMap = levelMap;
      }

      @Override
      public void format(final LogEvent event, final StringBuilder output) {
         output.append(this.levelMap.get(event.getLevel()));
      }
   }

   private static final class SimpleLevelPatternConverter extends LevelPatternConverter {
      @Override
      public void format(final LogEvent event, final StringBuilder output) {
         output.append(event.getLevel());
      }
   }
}
