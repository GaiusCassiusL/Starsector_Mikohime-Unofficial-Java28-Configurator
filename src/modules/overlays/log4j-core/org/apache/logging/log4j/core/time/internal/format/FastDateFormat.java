package org.apache.logging.log4j.core.time.internal.format;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FastDateFormat extends Format implements DateParser, DatePrinter {
   public static final int FULL = 0;
   public static final int LONG = 1;
   public static final int MEDIUM = 2;
   public static final int SHORT = 3;
   private static final FormatCache<FastDateFormat> cache = new FormatCache<FastDateFormat>() {
      protected FastDateFormat createInstance(final String pattern, final TimeZone timeZone, final Locale locale) {
         return new FastDateFormat(pattern, timeZone, locale);
      }
   };
   private final FastDatePrinter printer;
   private final FastDateParser parser;

   public static FastDateFormat getInstance() {
      return cache.getInstance();
   }

   public static FastDateFormat getInstance(final String pattern) {
      return cache.getInstance(pattern, null, null);
   }

   public static FastDateFormat getInstance(final String pattern, final TimeZone timeZone) {
      return cache.getInstance(pattern, timeZone, null);
   }

   public static FastDateFormat getInstance(final String pattern, final Locale locale) {
      return cache.getInstance(pattern, null, locale);
   }

   public static FastDateFormat getInstance(final String pattern, final TimeZone timeZone, final Locale locale) {
      return cache.getInstance(pattern, timeZone, locale);
   }

   public static FastDateFormat getDateInstance(final int style) {
      return cache.getDateInstance(style, null, null);
   }

   public static FastDateFormat getDateInstance(final int style, final Locale locale) {
      return cache.getDateInstance(style, null, locale);
   }

   public static FastDateFormat getDateInstance(final int style, final TimeZone timeZone) {
      return cache.getDateInstance(style, timeZone, null);
   }

   public static FastDateFormat getDateInstance(final int style, final TimeZone timeZone, final Locale locale) {
      return cache.getDateInstance(style, timeZone, locale);
   }

   public static FastDateFormat getTimeInstance(final int style) {
      return cache.getTimeInstance(style, null, null);
   }

   public static FastDateFormat getTimeInstance(final int style, final Locale locale) {
      return cache.getTimeInstance(style, null, locale);
   }

   public static FastDateFormat getTimeInstance(final int style, final TimeZone timeZone) {
      return cache.getTimeInstance(style, timeZone, null);
   }

   public static FastDateFormat getTimeInstance(final int style, final TimeZone timeZone, final Locale locale) {
      return cache.getTimeInstance(style, timeZone, locale);
   }

   public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle) {
      return cache.getDateTimeInstance(dateStyle, timeStyle, null, null);
   }

   public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle, final Locale locale) {
      return cache.getDateTimeInstance(dateStyle, timeStyle, null, locale);
   }

   public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle, final TimeZone timeZone) {
      return getDateTimeInstance(dateStyle, timeStyle, timeZone, null);
   }

   public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle, final TimeZone timeZone, final Locale locale) {
      return cache.getDateTimeInstance(dateStyle, timeStyle, timeZone, locale);
   }

   public static FastDateFormat getDateTimeInstance(final String pattern, final TimeZone timeZone, final Locale locale, final Date centuryStart) {
      return new FastDateFormat(pattern, timeZone, locale, centuryStart);
   }

   protected FastDateFormat(final String pattern, final TimeZone timeZone, final Locale locale) {
      this(pattern, timeZone, locale, null);
   }

   protected FastDateFormat(final String pattern, final TimeZone timeZone, final Locale locale, final Date centuryStart) {
      this.printer = new FastDatePrinter(pattern, timeZone, locale);
      this.parser = new FastDateParser(pattern, timeZone, locale, centuryStart);
   }

   @Override
   public StringBuilder format(final Object obj, final StringBuilder toAppendTo, final FieldPosition pos) {
      return toAppendTo.append(this.printer.format(obj));
   }

   @Override
   public String format(final long millis) {
      return this.printer.format(millis);
   }

   @Override
   public String format(final Date date) {
      return this.printer.format(date);
   }

   @Override
   public String format(final Calendar calendar) {
      return this.printer.format(calendar);
   }

   @Override
   public <B extends Appendable> B format(final long millis, final B buf) {
      return this.printer.format(millis, buf);
   }

   @Override
   public <B extends Appendable> B format(final Date date, final B buf) {
      return this.printer.format(date, buf);
   }

   @Override
   public <B extends Appendable> B format(final Calendar calendar, final B buf) {
      return this.printer.format(calendar, buf);
   }

   @Override
   public Date parse(final String source) throws ParseException {
      return this.parser.parse(source);
   }

   @Override
   public Date parse(final String source, final ParsePosition pos) {
      return this.parser.parse(source, pos);
   }

   @Override
   public boolean parse(final String source, final ParsePosition pos, final Calendar calendar) {
      return this.parser.parse(source, pos, calendar);
   }

   @Override
   public Object parseObject(final String source, final ParsePosition pos) {
      return this.parser.parseObject(source, pos);
   }

   @Override
   public String getPattern() {
      return this.printer.getPattern();
   }

   @Override
   public TimeZone getTimeZone() {
      return this.printer.getTimeZone();
   }

   @Override
   public Locale getLocale() {
      return this.printer.getLocale();
   }

   public int getMaxLengthEstimate() {
      return this.printer.getMaxLengthEstimate();
   }

   @Override
   public boolean equals(final Object obj) {
      if (!(obj instanceof FastDateFormat)) {
         return false;
      }

      FastDateFormat other = (FastDateFormat)obj;
      return this.printer.equals(other.printer);
   }

   @Override
   public int hashCode() {
      return this.printer.hashCode();
   }

   @Override
   public String toString() {
      return "FastDateFormat[" + this.printer.getPattern() + "," + this.printer.getLocale() + "," + this.printer.getTimeZone().getID() + "]";
   }
}
