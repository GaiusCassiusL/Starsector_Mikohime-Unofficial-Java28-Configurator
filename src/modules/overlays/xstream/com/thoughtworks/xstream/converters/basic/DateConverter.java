package com.thoughtworks.xstream.converters.basic;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.ErrorReporter;
import com.thoughtworks.xstream.converters.ErrorWriter;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.ThreadSafeSimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DateConverter extends AbstractSingleValueConverter implements ErrorReporter {
   private static final String[] DEFAULT_ACCEPTABLE_FORMATS;
   private static final String DEFAULT_PATTERN;
   private static final String DEFAULT_ERA_PATTERN;
   private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
   private static final long ERA_START;
   private final ThreadSafeSimpleDateFormat defaultFormat;
   private final ThreadSafeSimpleDateFormat defaultEraFormat;
   private final ThreadSafeSimpleDateFormat[] acceptableFormats;

   public DateConverter() {
      this(false);
   }

   public DateConverter(TimeZone timeZone) {
      this(DEFAULT_PATTERN, DEFAULT_ACCEPTABLE_FORMATS, timeZone);
   }

   public DateConverter(boolean lenient) {
      this(DEFAULT_PATTERN, DEFAULT_ACCEPTABLE_FORMATS, lenient);
   }

   public DateConverter(String defaultFormat, String[] acceptableFormats) {
      this(defaultFormat, acceptableFormats, false);
   }

   public DateConverter(String defaultFormat, String[] acceptableFormats, TimeZone timeZone) {
      this(defaultFormat, acceptableFormats, timeZone, false);
   }

   public DateConverter(String defaultFormat, String[] acceptableFormats, boolean lenient) {
      this(defaultFormat, acceptableFormats, UTC, lenient);
   }

   public DateConverter(String defaultFormat, String[] acceptableFormats, TimeZone timeZone, boolean lenient) {
      this(DEFAULT_ERA_PATTERN, defaultFormat, acceptableFormats, Locale.ENGLISH, timeZone, lenient);
   }

   public DateConverter(String defaultEraFormat, String defaultFormat, String[] acceptableFormats, Locale locale, TimeZone timeZone, boolean lenient) {
      if (defaultEraFormat != null) {
         this.defaultEraFormat = new ThreadSafeSimpleDateFormat(defaultEraFormat, timeZone, locale, 4, 20, lenient);
      } else {
         this.defaultEraFormat = null;
      }

      this.defaultFormat = new ThreadSafeSimpleDateFormat(defaultFormat, timeZone, locale, 4, 20, lenient);
      this.acceptableFormats = acceptableFormats != null ? new ThreadSafeSimpleDateFormat[acceptableFormats.length] : new ThreadSafeSimpleDateFormat[0];

      for (int i = 0; i < this.acceptableFormats.length; i++) {
         this.acceptableFormats[i] = new ThreadSafeSimpleDateFormat(acceptableFormats[i], timeZone, locale, 1, 20, lenient);
      }
   }

   public boolean canConvert(Class type) {
      return type == Date.class;
   }

   public Object fromString(String str) {
      if (this.defaultEraFormat != null) {
         try {
            return this.defaultEraFormat.parse(str);
         } catch (ParseException var6) {
         }
      }

      if (this.defaultEraFormat != this.defaultFormat) {
         try {
            return this.defaultFormat.parse(str);
         } catch (ParseException var5) {
         }
      }

      for (int i = 0; i < this.acceptableFormats.length; i++) {
         try {
            return this.acceptableFormats[i].parse(str);
         } catch (ParseException var4) {
         }
      }

      ConversionException exception = new ConversionException("Cannot parse date");
      exception.add("date", str);
      throw exception;
   }

   public String toString(Object obj) {
      Date date = (Date)obj;
      return date.getTime() < ERA_START && this.defaultEraFormat != null ? this.defaultEraFormat.format(date) : this.defaultFormat.format(date);
   }

   public void appendErrors(ErrorWriter errorWriter) {
      errorWriter.add("Default date pattern", this.defaultFormat.toString());
      if (this.defaultEraFormat != null) {
         errorWriter.add("Default era date pattern", this.defaultEraFormat.toString());
      }

      for (int i = 0; i < this.acceptableFormats.length; i++) {
         errorWriter.add("Alternative date pattern", this.acceptableFormats[i].toString());
      }
   }

   static {
      String defaultPattern = "yyyy-MM-dd HH:mm:ss.S z";
      String defaultEraPattern = "yyyy-MM-dd G HH:mm:ss.S z";
      List acceptablePatterns = new ArrayList();
      boolean utcSupported = JVM.canParseUTCDateFormat();
      DEFAULT_PATTERN = utcSupported ? "yyyy-MM-dd HH:mm:ss.S z" : "yyyy-MM-dd HH:mm:ss.S 'UTC'";
      DEFAULT_ERA_PATTERN = utcSupported ? "yyyy-MM-dd G HH:mm:ss.S z" : "yyyy-MM-dd G HH:mm:ss.S 'UTC'";
      acceptablePatterns.add("yyyy-MM-dd HH:mm:ss.S z");
      if (!utcSupported) {
         acceptablePatterns.add("yyyy-MM-dd HH:mm:ss.S z");
      }

      acceptablePatterns.add("yyyy-MM-dd HH:mm:ss.S a");
      acceptablePatterns.add("yyyy-MM-dd HH:mm:ssz");
      acceptablePatterns.add("yyyy-MM-dd HH:mm:ss z");
      if (!utcSupported) {
         acceptablePatterns.add("yyyy-MM-dd HH:mm:ss 'UTC'");
      }

      if (JVM.canParseISO8601TimeZoneInDateFormat()) {
         acceptablePatterns.add("yyyy-MM-dd'T'HH:mm:ss.SX");
         acceptablePatterns.add("yyyy-MM-dd'T'HH:mm:ssX");
         acceptablePatterns.add("yyyy-MM-dd'T'HH:mmX");
      }

      acceptablePatterns.add("yyyy-MM-dd HH:mm:ssa");
      DEFAULT_ACCEPTABLE_FORMATS = acceptablePatterns.toArray(new String[acceptablePatterns.size()]);
      Calendar cal = Calendar.getInstance();
      cal.setTimeZone(UTC);
      cal.clear();
      cal.set(1, 0, 1);
      ERA_START = cal.getTime().getTime();
   }
}
