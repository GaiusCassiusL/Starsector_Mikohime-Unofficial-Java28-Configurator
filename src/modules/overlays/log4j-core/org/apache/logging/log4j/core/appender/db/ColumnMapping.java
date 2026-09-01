package org.apache.logging.log4j.core.appender.db;

import java.util.Date;
import java.util.Locale;
import java.util.function.Supplier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

@Configurable(printObject = true)
@Plugin
public final class ColumnMapping {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final StringLayout layout;
   private final String literalValue;
   private final String name;
   private final String nameKey;
   private final String parameter;
   private final String source;
   private final Class<?> type;
   private final Supplier<TypeConverter<?>> typeConverter;

   @PluginFactory
   public static ColumnMapping.Builder newBuilder() {
      return new ColumnMapping.Builder();
   }

   public static String toKey(final String name) {
      return name.toUpperCase(Locale.ROOT);
   }

   private ColumnMapping(
      final String name,
      final String source,
      final StringLayout layout,
      final String literalValue,
      final String parameter,
      final Class<?> type,
      final Supplier<TypeConverter<?>> typeConverter
   ) {
      this.name = name;
      this.nameKey = toKey(name);
      this.source = source;
      this.layout = layout;
      this.literalValue = literalValue;
      this.parameter = parameter;
      this.type = type;
      this.typeConverter = typeConverter;
   }

   public StringLayout getLayout() {
      return this.layout;
   }

   public String getLiteralValue() {
      return this.literalValue;
   }

   public String getName() {
      return this.name;
   }

   public String getNameKey() {
      return this.nameKey;
   }

   public String getParameter() {
      return this.parameter;
   }

   public String getSource() {
      return this.source;
   }

   public Class<?> getType() {
      return this.type;
   }

   public TypeConverter<?> getTypeConverter() {
      return this.typeConverter.get();
   }

   @Override
   public String toString() {
      return "ColumnMapping [name="
         + this.name
         + ", source="
         + this.source
         + ", literalValue="
         + this.literalValue
         + ", parameter="
         + this.parameter
         + ", type="
         + this.type
         + ", layout="
         + this.layout
         + "]";
   }

   public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<ColumnMapping> {
      @PluginConfiguration
      private Configuration configuration;
      @PluginElement("Layout")
      private StringLayout layout;
      @PluginBuilderAttribute
      private String literal;
      @PluginBuilderAttribute
      @Required(message = "No column name provided")
      private String name;
      @PluginBuilderAttribute
      private String parameter;
      @PluginBuilderAttribute
      private String pattern;
      @PluginBuilderAttribute
      private String source;
      @PluginBuilderAttribute
      @Required(message = "No conversion type provided")
      private Class<?> type = String.class;
      private Injector injector;

      public ColumnMapping build() {
         if (this.pattern != null) {
            this.layout = PatternLayout.newBuilder().setPattern(this.pattern).setConfiguration(this.configuration).setAlwaysWriteExceptions(false).build();
         }

         if (this.layout != null
            && this.literal != null
            && !Date.class.isAssignableFrom(this.type)
            && !ReadOnlyStringMap.class.isAssignableFrom(this.type)
            && !ThreadContextMap.class.isAssignableFrom(this.type)
            && !ThreadContextStack.class.isAssignableFrom(this.type)) {
            ColumnMapping.LOGGER
               .error(
                  "No 'layout' or 'literal' value specified and type ({}) is not compatible with ThreadContextMap, ThreadContextStack, or java.util.Date for the mapping {}",
                  this.type,
                  this
               );
            return null;
         } else if (this.literal != null && this.parameter != null) {
            ColumnMapping.LOGGER.error("Only one of 'literal' or 'parameter' can be set on the column mapping {}", this);
            return null;
         } else {
            return new ColumnMapping(
               this.name, this.source, this.layout, this.literal, this.parameter, this.type, () -> this.injector.getTypeConverter(this.type)
            );
         }
      }

      public ColumnMapping.Builder setConfiguration(final Configuration configuration) {
         this.configuration = configuration;
         return this;
      }

      public ColumnMapping.Builder setLayout(final StringLayout layout) {
         this.layout = layout;
         return this;
      }

      public ColumnMapping.Builder setLiteral(final String literal) {
         this.literal = literal;
         return this;
      }

      public ColumnMapping.Builder setName(final String name) {
         this.name = name;
         return this;
      }

      public ColumnMapping.Builder setParameter(final String parameter) {
         this.parameter = parameter;
         return this;
      }

      public ColumnMapping.Builder setPattern(final String pattern) {
         this.pattern = pattern;
         return this;
      }

      public ColumnMapping.Builder setSource(final String source) {
         this.source = source;
         return this;
      }

      public ColumnMapping.Builder setType(final Class<?> type) {
         this.type = type;
         return this;
      }

      @Inject
      public ColumnMapping.Builder setInjector(final Injector injector) {
         this.injector = injector;
         return this;
      }

      @Override
      public String toString() {
         return "Builder [name="
            + this.name
            + ", source="
            + this.source
            + ", literal="
            + this.literal
            + ", parameter="
            + this.parameter
            + ", pattern="
            + this.pattern
            + ", type="
            + this.type
            + ", layout="
            + this.layout
            + "]";
      }
   }
}
