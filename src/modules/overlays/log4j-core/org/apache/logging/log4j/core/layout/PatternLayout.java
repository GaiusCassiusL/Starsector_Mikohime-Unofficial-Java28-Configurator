package org.apache.logging.log4j.core.layout;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.pattern.FormattingInfo;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.pattern.RegexReplacement;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.Strings;

@Configurable(elementType = "layout", printObject = true)
@Plugin
public final class PatternLayout extends AbstractStringLayout {
   public static final String DEFAULT_CONVERSION_PATTERN = "%m%n";
   public static final String TTCC_CONVERSION_PATTERN = "%r [%t] %p %c %notEmpty{%x }- %m%n";
   public static final String SIMPLE_CONVERSION_PATTERN = "%d [%t] %p %c - %m%n";
   public static final String KEY = "Converter";
   private final String conversionPattern;
   private final PatternSelector patternSelector;
   private final AbstractStringLayout.Serializer eventSerializer;

   private PatternLayout(
      final Configuration config,
      final RegexReplacement replace,
      final String eventPattern,
      final PatternSelector patternSelector,
      final Charset charset,
      final boolean alwaysWriteExceptions,
      final boolean disableAnsi,
      final boolean noConsoleNoAnsi,
      final String headerPattern,
      final String footerPattern
   ) {
      super(
         config,
         charset,
         newSerializerBuilder()
            .setConfiguration(config)
            .setReplace(replace)
            .setPatternSelector(patternSelector)
            .setAlwaysWriteExceptions(alwaysWriteExceptions)
            .setDisableAnsi(disableAnsi)
            .setNoConsoleNoAnsi(noConsoleNoAnsi)
            .setPattern(headerPattern)
            .build(),
         newSerializerBuilder()
            .setConfiguration(config)
            .setReplace(replace)
            .setPatternSelector(patternSelector)
            .setAlwaysWriteExceptions(alwaysWriteExceptions)
            .setDisableAnsi(disableAnsi)
            .setNoConsoleNoAnsi(noConsoleNoAnsi)
            .setPattern(footerPattern)
            .build()
      );
      this.conversionPattern = eventPattern;
      this.patternSelector = patternSelector;
      this.eventSerializer = newSerializerBuilder()
         .setConfiguration(config)
         .setReplace(replace)
         .setPatternSelector(patternSelector)
         .setAlwaysWriteExceptions(alwaysWriteExceptions)
         .setDisableAnsi(disableAnsi)
         .setNoConsoleNoAnsi(noConsoleNoAnsi)
         .setPattern(eventPattern)
         .setDefaultPattern("%m%n")
         .build();
   }

   public static PatternLayout.SerializerBuilder newSerializerBuilder() {
      return new PatternLayout.SerializerBuilder();
   }

   @Override
   public boolean requiresLocation() {
      return this.eventSerializer.requiresLocation();
   }

   public String getConversionPattern() {
      return this.conversionPattern;
   }

   @Override
   public Map<String, String> getContentFormat() {
      Map<String, String> result = new HashMap<>();
      result.put("structured", "false");
      result.put("formatType", "conversion");
      result.put("format", this.conversionPattern);
      return result;
   }

   @Override
   public String toSerializable(final LogEvent event) {
      return this.eventSerializer.toSerializable(event);
   }

   public void serialize(final LogEvent event, final StringBuilder stringBuilder) {
      this.eventSerializer.toSerializable(event, stringBuilder);
   }

   @Override
   public void encode(final LogEvent event, final ByteBufferDestination destination) {
      StringBuilder text = this.toText(this.eventSerializer, event, getStringBuilder());
      Encoder<StringBuilder> encoder = this.getStringBuilderEncoder();
      encoder.encode(text, destination);
      trimToMaxSize(text);
   }

   private StringBuilder toText(final AbstractStringLayout.Serializer2 serializer, final LogEvent event, final StringBuilder destination) {
      return serializer.toSerializable(event, destination);
   }

   public static PatternParser createPatternParser(final Configuration config) {
      if (config == null) {
         return new PatternParser(config, "Converter", LogEventPatternConverter.class);
      }

      PatternParser parser = config.getComponent("Converter");
      if (parser == null) {
         parser = new PatternParser(config, "Converter", LogEventPatternConverter.class);
         config.addComponent("Converter", parser);
         parser = config.getComponent("Converter");
      }

      return parser;
   }

   @Override
   public String toString() {
      return this.patternSelector == null ? this.conversionPattern : this.patternSelector.toString();
   }

   public static PatternLayout createDefaultLayout() {
      return newBuilder().build();
   }

   public static PatternLayout createDefaultLayout(final Configuration configuration) {
      return newBuilder().setConfiguration(configuration).build();
   }

   @PluginFactory
   public static PatternLayout.Builder newBuilder() {
      return new PatternLayout.Builder();
   }

   public AbstractStringLayout.Serializer getEventSerializer() {
      return this.eventSerializer;
   }

   public static final class Builder implements org.apache.logging.log4j.plugins.util.Builder<PatternLayout> {
      @PluginBuilderAttribute
      private String pattern = "%m%n";
      @PluginElement("PatternSelector")
      private PatternSelector patternSelector;
      @PluginConfiguration
      private Configuration configuration;
      @PluginElement("Replace")
      private RegexReplacement regexReplacement;
      @PluginBuilderAttribute
      private Charset charset = Charset.defaultCharset();
      @PluginBuilderAttribute
      private boolean alwaysWriteExceptions = true;
      @PluginBuilderAttribute
      private boolean disableAnsi = !this.useAnsiEscapeCodes();
      @PluginBuilderAttribute
      private boolean noConsoleNoAnsi;
      @PluginBuilderAttribute
      private String header;
      @PluginBuilderAttribute
      private String footer;

      private Builder() {
      }

      private boolean useAnsiEscapeCodes() {
         PropertyEnvironment properties = PropertiesUtil.getProperties();
         boolean isPlatformSupportsAnsi = !properties.isOsWindows();
         boolean isJansiRequested = !properties.getBooleanProperty(Log4jPropertyKey.CONSOLE_JANSI_ENABLED, false);
         return isPlatformSupportsAnsi || isJansiRequested;
      }

      public PatternLayout.Builder setPattern(final String pattern) {
         this.pattern = pattern;
         return this;
      }

      public PatternLayout.Builder setPatternSelector(final PatternSelector patternSelector) {
         this.patternSelector = patternSelector;
         return this;
      }

      public PatternLayout.Builder setConfiguration(final Configuration configuration) {
         this.configuration = configuration;
         return this;
      }

      public PatternLayout.Builder setRegexReplacement(final RegexReplacement regexReplacement) {
         this.regexReplacement = regexReplacement;
         return this;
      }

      public PatternLayout.Builder setCharset(final Charset charset) {
         if (charset != null) {
            this.charset = charset;
         }

         return this;
      }

      public PatternLayout.Builder setAlwaysWriteExceptions(final boolean alwaysWriteExceptions) {
         this.alwaysWriteExceptions = alwaysWriteExceptions;
         return this;
      }

      public PatternLayout.Builder setDisableAnsi(final boolean disableAnsi) {
         this.disableAnsi = disableAnsi;
         return this;
      }

      public PatternLayout.Builder setNoConsoleNoAnsi(final boolean noConsoleNoAnsi) {
         this.noConsoleNoAnsi = noConsoleNoAnsi;
         return this;
      }

      public PatternLayout.Builder setHeader(final String header) {
         this.header = header;
         return this;
      }

      public PatternLayout.Builder setFooter(final String footer) {
         this.footer = footer;
         return this;
      }

      public PatternLayout build() {
         if (this.configuration == null) {
            this.configuration = new DefaultConfiguration();
         }

         return new PatternLayout(
            this.configuration,
            this.regexReplacement,
            this.pattern,
            this.patternSelector,
            this.charset,
            this.alwaysWriteExceptions,
            this.disableAnsi,
            this.noConsoleNoAnsi,
            this.header,
            this.footer
         );
      }
   }

   private static final class NoFormatPatternSerializer implements PatternLayout.PatternSerializer {
      private final LogEventPatternConverter[] converters;

      private NoFormatPatternSerializer(final PatternFormatter[] formatters) {
         this.converters = new LogEventPatternConverter[formatters.length];

         for (int i = 0; i < formatters.length; i++) {
            this.converters[i] = formatters[i].getConverter();
         }
      }

      @Override
      public String toSerializable(final LogEvent event) {
         StringBuilder sb = AbstractStringLayout.getStringBuilder();

         try {
            return this.toSerializable(event, sb).toString();
         } finally {
            AbstractStringLayout.trimToMaxSize(sb);
         }
      }

      @Override
      public StringBuilder toSerializable(final LogEvent event, final StringBuilder buffer) {
         for (LogEventPatternConverter converter : this.converters) {
            converter.format(event, buffer);
         }

         return buffer;
      }

      @Override
      public boolean requiresLocation() {
         for (LogEventPatternConverter converter : this.converters) {
            if (converter.requiresLocation()) {
               return true;
            }
         }

         return false;
      }

      @Override
      public String toString() {
         return super.toString() + "[converters=" + Arrays.toString(this.converters) + "]";
      }
   }

   private static final class PatternFormatterPatternSerializer implements PatternLayout.PatternSerializer {
      private final PatternFormatter[] formatters;

      private PatternFormatterPatternSerializer(final PatternFormatter[] formatters) {
         this.formatters = formatters;
      }

      @Override
      public String toSerializable(final LogEvent event) {
         StringBuilder sb = AbstractStringLayout.getStringBuilder();

         try {
            return this.toSerializable(event, sb).toString();
         } finally {
            AbstractStringLayout.trimToMaxSize(sb);
         }
      }

      @Override
      public StringBuilder toSerializable(final LogEvent event, final StringBuilder buffer) {
         for (PatternFormatter formatter : this.formatters) {
            formatter.format(event, buffer);
         }

         return buffer;
      }

      @Override
      public String toString() {
         return super.toString() + "[formatters=" + Arrays.toString(this.formatters) + "]";
      }
   }

   private static final class PatternSelectorSerializer implements AbstractStringLayout.Serializer, AbstractStringLayout.Serializer2 {
      private final PatternSelector patternSelector;
      private final RegexReplacement replace;

      private PatternSelectorSerializer(final PatternSelector patternSelector, final RegexReplacement replace) {
         this.patternSelector = patternSelector;
         this.replace = replace;
      }

      @Override
      public String toSerializable(final LogEvent event) {
         StringBuilder sb = AbstractStringLayout.getStringBuilder();

         try {
            return this.toSerializable(event, sb).toString();
         } finally {
            AbstractStringLayout.trimToMaxSize(sb);
         }
      }

      @Override
      public StringBuilder toSerializable(final LogEvent event, final StringBuilder buffer) {
         for (PatternFormatter formatter : this.patternSelector.getFormatters(event)) {
            formatter.format(event, buffer);
         }

         if (this.replace != null) {
            String str = buffer.toString();
            str = this.replace.format(str);
            buffer.setLength(0);
            buffer.append(str);
         }

         return buffer;
      }

      @Override
      public boolean requiresLocation() {
         return this.patternSelector.requiresLocation();
      }

      @Override
      public String toString() {
         StringBuilder builder = new StringBuilder();
         builder.append(super.toString());
         builder.append("[patternSelector=");
         builder.append(this.patternSelector);
         builder.append(", replace=");
         builder.append(this.replace);
         builder.append("]");
         return builder.toString();
      }
   }

   private interface PatternSerializer extends AbstractStringLayout.Serializer, AbstractStringLayout.Serializer2 {
   }

   private static final class PatternSerializerWithReplacement implements AbstractStringLayout.Serializer, AbstractStringLayout.Serializer2 {
      private final PatternLayout.PatternSerializer delegate;
      private final RegexReplacement replace;

      private PatternSerializerWithReplacement(final PatternLayout.PatternSerializer delegate, final RegexReplacement replace) {
         this.delegate = delegate;
         this.replace = replace;
      }

      @Override
      public String toSerializable(final LogEvent event) {
         StringBuilder sb = AbstractStringLayout.getStringBuilder();

         try {
            return this.toSerializable(event, sb).toString();
         } finally {
            AbstractStringLayout.trimToMaxSize(sb);
         }
      }

      @Override
      public StringBuilder toSerializable(final LogEvent event, final StringBuilder buf) {
         StringBuilder buffer = this.delegate.toSerializable(event, buf);
         String str = buffer.toString();
         str = this.replace.format(str);
         buffer.setLength(0);
         buffer.append(str);
         return buffer;
      }

      @Override
      public String toString() {
         return super.toString() + "[delegate=" + this.delegate + ", replace=" + this.replace + "]";
      }

      @Override
      public boolean requiresLocation() {
         return this.delegate.requiresLocation();
      }
   }

   public static class SerializerBuilder implements org.apache.logging.log4j.plugins.util.Builder<AbstractStringLayout.Serializer> {
      private Configuration configuration;
      private RegexReplacement replace;
      private String pattern;
      private String defaultPattern;
      private PatternSelector patternSelector;
      private boolean alwaysWriteExceptions;
      private boolean disableAnsi;
      private boolean noConsoleNoAnsi;

      public AbstractStringLayout.Serializer build() {
         if (Strings.isEmpty(this.pattern) && Strings.isEmpty(this.defaultPattern)) {
            return null;
         }

         if (this.patternSelector == null) {
            try {
               PatternParser parser = PatternLayout.createPatternParser(this.configuration);
               List<PatternFormatter> list = parser.parse(
                  this.pattern == null ? this.defaultPattern : this.pattern, this.alwaysWriteExceptions, this.disableAnsi, this.noConsoleNoAnsi
               );
               PatternFormatter[] formatters = list.toArray(new PatternFormatter[0]);
               boolean hasFormattingInfo = false;

               for (PatternFormatter formatter : formatters) {
                  FormattingInfo info = formatter.getFormattingInfo();
                  if (info != null && info != FormattingInfo.getDefault()) {
                     hasFormattingInfo = true;
                     break;
                  }
               }

               PatternLayout.PatternSerializer serializer = hasFormattingInfo
                  ? new PatternLayout.PatternFormatterPatternSerializer(formatters)
                  : new PatternLayout.NoFormatPatternSerializer(formatters);
               return this.replace == null ? serializer : new PatternLayout.PatternSerializerWithReplacement(serializer, this.replace);
            } catch (RuntimeException ex) {
               throw new IllegalArgumentException("Cannot parse pattern '" + this.pattern + "'", ex);
            }
         } else {
            return new PatternLayout.PatternSelectorSerializer(this.patternSelector, this.replace);
         }
      }

      public PatternLayout.SerializerBuilder setConfiguration(final Configuration configuration) {
         this.configuration = configuration;
         return this;
      }

      public PatternLayout.SerializerBuilder setReplace(final RegexReplacement replace) {
         this.replace = replace;
         return this;
      }

      public PatternLayout.SerializerBuilder setPattern(final String pattern) {
         this.pattern = pattern;
         return this;
      }

      public PatternLayout.SerializerBuilder setDefaultPattern(final String defaultPattern) {
         this.defaultPattern = defaultPattern;
         return this;
      }

      public PatternLayout.SerializerBuilder setPatternSelector(final PatternSelector patternSelector) {
         this.patternSelector = patternSelector;
         return this;
      }

      public PatternLayout.SerializerBuilder setAlwaysWriteExceptions(final boolean alwaysWriteExceptions) {
         this.alwaysWriteExceptions = alwaysWriteExceptions;
         return this;
      }

      public PatternLayout.SerializerBuilder setDisableAnsi(final boolean disableAnsi) {
         this.disableAnsi = disableAnsi;
         return this;
      }

      public PatternLayout.SerializerBuilder setNoConsoleNoAnsi(final boolean noConsoleNoAnsi) {
         this.noConsoleNoAnsi = noConsoleNoAnsi;
         return this;
      }
   }
}
