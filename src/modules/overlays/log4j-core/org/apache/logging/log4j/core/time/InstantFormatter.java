package org.apache.logging.log4j.core.time;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import org.apache.logging.log4j.core.time.internal.format.FastDateFormat;
import org.apache.logging.log4j.core.time.internal.format.FixedDateFormat;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

public final class InstantFormatter {
   private static final StatusLogger LOGGER = StatusLogger.getLogger();
   private static final InstantFormatter.FormatterFactory[] FORMATTER_FACTORIES = new InstantFormatter.FormatterFactory[]{
      new InstantFormatter.Log4jFixedFormatterFactory(), new InstantFormatter.Log4jFastFormatterFactory(), new InstantFormatter.JavaDateTimeFormatterFactory()
   };
   private final InstantFormatter.Formatter formatter;

   private InstantFormatter(final InstantFormatter.Builder builder) {
      this.formatter = Arrays.stream(FORMATTER_FACTORIES).map(formatterFactory -> {
         try {
            return formatterFactory.createIfSupported(builder.getPattern(), builder.getLocale(), builder.getTimeZone());
         } catch (Exception error) {
            LOGGER.warn("skipping the failed formatter factory \"{}\"", formatterFactory, error);
            return null;
         }
      }).filter(Objects::nonNull).findFirst().orElseThrow(() -> new AssertionError("could not find a matching formatter"));
   }

   public String format(final Instant instant) {
      Objects.requireNonNull(instant, "instant");
      StringBuilder stringBuilder = new StringBuilder();
      this.formatter.format(instant, stringBuilder);
      return stringBuilder.toString();
   }

   public void format(final Instant instant, final StringBuilder stringBuilder) {
      Objects.requireNonNull(instant, "instant");
      Objects.requireNonNull(stringBuilder, "stringBuilder");
      this.formatter.format(instant, stringBuilder);
   }

   public boolean isInstantMatching(final Instant instant1, final Instant instant2) {
      return this.formatter.isInstantMatching(instant1, instant2);
   }

   public Class<?> getInternalImplementationClass() {
      return this.formatter.getInternalImplementationClass();
   }

   public static InstantFormatter.Builder newBuilder() {
      return new InstantFormatter.Builder();
   }

   private static boolean patternSupported(final String pattern, final Locale locale, final TimeZone timeZone, final InstantFormatter.Formatter formatter) {
      DateTimeFormatter javaFormatter = DateTimeFormatter.ofPattern(pattern).withLocale(locale).withZone(timeZone.toZoneId());
      MutableInstant instant = new MutableInstant();
      instant.initFromEpochSecond(1621280470L, 123456789);
      String expectedFormat = javaFormatter.format(instant);
      StringBuilder stringBuilder = new StringBuilder();
      formatter.format(instant, stringBuilder);
      String actualFormat = stringBuilder.toString();
      return expectedFormat.equals(actualFormat);
   }

   public static final class Builder {
      private String pattern;
      private Locale locale = Locale.getDefault();
      private TimeZone timeZone = TimeZone.getDefault();

      private Builder() {
      }

      public String getPattern() {
         return this.pattern;
      }

      public InstantFormatter.Builder setPattern(final String pattern) {
         this.pattern = pattern;
         return this;
      }

      public Locale getLocale() {
         return this.locale;
      }

      public InstantFormatter.Builder setLocale(final Locale locale) {
         this.locale = locale;
         return this;
      }

      public TimeZone getTimeZone() {
         return this.timeZone;
      }

      public InstantFormatter.Builder setTimeZone(final TimeZone timeZone) {
         this.timeZone = timeZone;
         return this;
      }

      public InstantFormatter build() {
         this.validate();
         return new InstantFormatter(this);
      }

      private void validate() {
         if (Strings.isBlank(this.pattern)) {
            throw new IllegalArgumentException("blank pattern");
         }

         Objects.requireNonNull(this.locale, "locale");
         Objects.requireNonNull(this.timeZone, "timeZone");
      }
   }

   private interface Formatter {
      Class<?> getInternalImplementationClass();

      void format(Instant instant, StringBuilder stringBuilder);

      boolean isInstantMatching(Instant instant1, Instant instant2);
   }

   private interface FormatterFactory {
      InstantFormatter.Formatter createIfSupported(String pattern, Locale locale, TimeZone timeZone);
   }

   private static final class JavaDateTimeFormatter implements InstantFormatter.Formatter {
      private final DateTimeFormatter formatter;
      private final MutableInstant mutableInstant;

      private JavaDateTimeFormatter(final String pattern, final Locale locale, final TimeZone timeZone) {
         this.formatter = DateTimeFormatter.ofPattern(pattern).withLocale(locale).withZone(timeZone.toZoneId());
         this.mutableInstant = new MutableInstant();
      }

      @Override
      public Class<?> getInternalImplementationClass() {
         return DateTimeFormatter.class;
      }

      @Override
      public void format(final Instant instant, final StringBuilder stringBuilder) {
         if (instant instanceof MutableInstant) {
            this.formatMutableInstant((MutableInstant)instant, stringBuilder);
         } else {
            this.formatInstant(instant, stringBuilder);
         }
      }

      private void formatMutableInstant(final MutableInstant instant, final StringBuilder stringBuilder) {
         this.formatter.formatTo(instant, stringBuilder);
      }

      private void formatInstant(final Instant instant, final StringBuilder stringBuilder) {
         this.mutableInstant.initFrom(instant);
         this.formatMutableInstant(this.mutableInstant, stringBuilder);
      }

      @Override
      public boolean isInstantMatching(final Instant instant1, final Instant instant2) {
         return instant1.getEpochSecond() == instant2.getEpochSecond() && instant1.getNanoOfSecond() == instant2.getNanoOfSecond();
      }
   }

   private static final class JavaDateTimeFormatterFactory implements InstantFormatter.FormatterFactory {
      @Override
      public InstantFormatter.Formatter createIfSupported(final String pattern, final Locale locale, final TimeZone timeZone) {
         return new InstantFormatter.JavaDateTimeFormatter(pattern, locale, timeZone);
      }
   }

   private static final class Log4jFastFormatter implements InstantFormatter.Formatter {
      private final FastDateFormat formatter;
      private final Calendar calendar;

      private Log4jFastFormatter(final String pattern, final Locale locale, final TimeZone timeZone) {
         this.formatter = FastDateFormat.getInstance(pattern, timeZone, locale);
         this.calendar = Calendar.getInstance(timeZone, locale);
      }

      @Override
      public Class<?> getInternalImplementationClass() {
         return FastDateFormat.class;
      }

      @Override
      public void format(final Instant instant, final StringBuilder stringBuilder) {
         this.calendar.setTimeInMillis(instant.getEpochMillisecond());
         this.formatter.format(this.calendar, stringBuilder);
      }

      @Override
      public boolean isInstantMatching(final Instant instant1, final Instant instant2) {
         return instant1.getEpochMillisecond() == instant2.getEpochMillisecond();
      }
   }

   private static final class Log4jFastFormatterFactory implements InstantFormatter.FormatterFactory {
      @Override
      public InstantFormatter.Formatter createIfSupported(final String pattern, final Locale locale, final TimeZone timeZone) {
         InstantFormatter.Log4jFastFormatter formatter = new InstantFormatter.Log4jFastFormatter(pattern, locale, timeZone);
         boolean patternSupported = InstantFormatter.patternSupported(pattern, locale, timeZone, formatter);
         return patternSupported ? formatter : null;
      }
   }

   private static final class Log4jFixedFormatter implements InstantFormatter.Formatter {
      private final FixedDateFormat formatter;
      private final char[] buffer;

      private Log4jFixedFormatter(final FixedDateFormat formatter) {
         this.formatter = formatter;
         this.buffer = new char[formatter.getFormat().length()];
      }

      @Override
      public Class<?> getInternalImplementationClass() {
         return FixedDateFormat.class;
      }

      @Override
      public void format(final Instant instant, final StringBuilder stringBuilder) {
         int length = this.formatter.formatInstant(instant, this.buffer, 0);
         stringBuilder.append(this.buffer, 0, length);
      }

      @Override
      public boolean isInstantMatching(final Instant instant1, final Instant instant2) {
         return this.formatter.isEquivalent(instant1.getEpochSecond(), instant1.getNanoOfSecond(), instant2.getEpochSecond(), instant2.getNanoOfSecond());
      }
   }

   private static final class Log4jFixedFormatterFactory implements InstantFormatter.FormatterFactory {
      @Override
      public InstantFormatter.Formatter createIfSupported(final String pattern, final Locale locale, final TimeZone timeZone) {
         FixedDateFormat internalFormatter = FixedDateFormat.createIfSupported(pattern, timeZone.getID());
         if (internalFormatter == null) {
            return null;
         }

         InstantFormatter.Log4jFixedFormatter formatter = new InstantFormatter.Log4jFixedFormatter(internalFormatter);
         boolean patternSupported = InstantFormatter.patternSupported(pattern, locale, timeZone, formatter);
         return patternSupported ? formatter : null;
      }
   }
}
