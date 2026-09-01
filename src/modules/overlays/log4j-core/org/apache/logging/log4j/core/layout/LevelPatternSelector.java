package org.apache.logging.log4j.core.layout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(elementType = "patternSelector", printObject = true)
@Plugin
public class LevelPatternSelector implements PatternSelector {
   private final Map<String, PatternFormatter[]> formatterMap = new HashMap<>();
   private final Map<String, String> patternMap = new HashMap<>();
   private final PatternFormatter[] defaultFormatters;
   private final String defaultPattern;
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final boolean requiresLocation;

   @Deprecated
   public LevelPatternSelector(
      final PatternMatch[] properties,
      final String defaultPattern,
      final boolean alwaysWriteExceptions,
      final boolean noConsoleNoAnsi,
      final Configuration config
   ) {
      this(properties, defaultPattern, alwaysWriteExceptions, false, noConsoleNoAnsi, config);
   }

   private LevelPatternSelector(
      final PatternMatch[] properties,
      final String defaultPattern,
      final boolean alwaysWriteExceptions,
      final boolean disableAnsi,
      final boolean noConsoleNoAnsi,
      final Configuration config
   ) {
      boolean needsLocation = false;
      PatternParser parser = PatternLayout.createPatternParser(config);

      for (PatternMatch property : properties) {
         try {
            List<PatternFormatter> list = parser.parse(property.getPattern(), alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
            PatternFormatter[] formatters = list.toArray(new PatternFormatter[0]);
            this.formatterMap.put(property.getKey(), formatters);

            for (int i = 0; !needsLocation && i < formatters.length; i++) {
               needsLocation = formatters[i].requiresLocation();
            }

            this.patternMap.put(property.getKey(), property.getPattern());
         } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Cannot parse pattern '" + property.getPattern() + "'", ex);
         }
      }

      try {
         List<PatternFormatter> list = parser.parse(defaultPattern, alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
         this.defaultFormatters = list.toArray(new PatternFormatter[0]);
         this.defaultPattern = defaultPattern;

         for (int i = 0; !needsLocation && i < this.defaultFormatters.length; i++) {
            needsLocation = this.defaultFormatters[i].requiresLocation();
         }
      } catch (RuntimeException ex) {
         throw new IllegalArgumentException("Cannot parse pattern '" + defaultPattern + "'", ex);
      }

      this.requiresLocation = needsLocation;
   }

   @Override
   public boolean requiresLocation() {
      return this.requiresLocation;
   }

   @Override
   public PatternFormatter[] getFormatters(final LogEvent event) {
      Level level = event.getLevel();
      if (level == null) {
         return this.defaultFormatters;
      }

      for (String key : this.formatterMap.keySet()) {
         if (level.name().equalsIgnoreCase(key)) {
            return this.formatterMap.get(key);
         }
      }

      return this.defaultFormatters;
   }

   @PluginBuilderFactory
   public static LevelPatternSelector.Builder newBuilder() {
      return new LevelPatternSelector.Builder();
   }

   @Deprecated
   public static LevelPatternSelector createSelector(
      final PatternMatch[] properties,
      final String defaultPattern,
      final boolean alwaysWriteExceptions,
      final boolean noConsoleNoAnsi,
      final Configuration configuration
   ) {
      LevelPatternSelector.Builder builder = newBuilder();
      builder.setProperties(properties);
      builder.setDefaultPattern(defaultPattern);
      builder.setAlwaysWriteExceptions(alwaysWriteExceptions);
      builder.setNoConsoleNoAnsi(noConsoleNoAnsi);
      builder.setConfiguration(configuration);
      return builder.build();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      boolean first = true;

      for (Entry<String, String> entry : this.patternMap.entrySet()) {
         if (!first) {
            sb.append(", ");
         }

         sb.append("key=\"").append(entry.getKey()).append("\", pattern=\"").append(entry.getValue()).append("\"");
         first = false;
      }

      if (!first) {
         sb.append(", ");
      }

      sb.append("default=\"").append(this.defaultPattern).append("\"");
      return sb.toString();
   }

   public static class Builder implements org.apache.logging.log4j.core.util.Builder<LevelPatternSelector> {
      @PluginElement("PatternMatch")
      private PatternMatch[] properties;
      @PluginBuilderAttribute("defaultPattern")
      private String defaultPattern;
      @PluginBuilderAttribute("alwaysWriteExceptions")
      private boolean alwaysWriteExceptions = true;
      @PluginBuilderAttribute("disableAnsi")
      private boolean disableAnsi;
      @PluginBuilderAttribute("noConsoleNoAnsi")
      private boolean noConsoleNoAnsi;
      @PluginConfiguration
      private Configuration configuration;

      public LevelPatternSelector build() {
         if (this.defaultPattern == null) {
            this.defaultPattern = "%m%n";
         }

         if (this.properties != null && this.properties.length != 0) {
            return new LevelPatternSelector(
               this.properties, this.defaultPattern, this.alwaysWriteExceptions, this.disableAnsi, this.noConsoleNoAnsi, this.configuration
            );
         }

         LevelPatternSelector.LOGGER.warn("No marker patterns were provided with PatternMatch");
         return null;
      }

      public LevelPatternSelector.Builder setProperties(final PatternMatch[] properties) {
         this.properties = properties;
         return this;
      }

      public LevelPatternSelector.Builder setDefaultPattern(final String defaultPattern) {
         this.defaultPattern = defaultPattern;
         return this;
      }

      public LevelPatternSelector.Builder setAlwaysWriteExceptions(final boolean alwaysWriteExceptions) {
         this.alwaysWriteExceptions = alwaysWriteExceptions;
         return this;
      }

      public LevelPatternSelector.Builder setDisableAnsi(final boolean disableAnsi) {
         this.disableAnsi = disableAnsi;
         return this;
      }

      public LevelPatternSelector.Builder setNoConsoleNoAnsi(final boolean noConsoleNoAnsi) {
         this.noConsoleNoAnsi = noConsoleNoAnsi;
         return this;
      }

      public LevelPatternSelector.Builder setConfiguration(final Configuration configuration) {
         this.configuration = configuration;
         return this;
      }
   }
}
