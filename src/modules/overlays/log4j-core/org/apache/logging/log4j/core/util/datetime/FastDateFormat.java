package org.apache.logging.log4j.core.util.datetime;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.logging.log4j.core.time.internal.format.DateParser;
import org.apache.logging.log4j.core.time.internal.format.DatePrinter;
import org.apache.logging.log4j.core.time.internal.format.Format;

/** @deprecated */
public class FastDateFormat extends Format implements DateParser, DatePrinter {
   private org.apache.logging.log4j.core.time.internal.format.FastDateFormat formatter = null;

   public static FastDateFormat getInstance() {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance());
   }

   public static FastDateFormat getInstance(final String pattern) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern));
   }

   public static FastDateFormat getInstance(final String pattern, final TimeZone timeZone) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern, timeZone));
   }

   public static FastDateFormat getInstance(final String pattern, final Locale locale) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern, locale));
   }

   public static FastDateFormat getInstance(final String pattern, final TimeZone timeZone, final Locale locale) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern, timeZone, locale));
   }

   public static FastDateFormat getDateInstance(final int style) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateInstance(style));
   }

   public static FastDateFormat getDateInstance(final int style, final Locale locale) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateInstance(style, locale));
   }

   public static FastDateFormat getDateInstance(final int style, final TimeZone timeZone) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateInstance(style, timeZone));
   }

   public static FastDateFormat getDateInstance(final int style, final TimeZone timeZone, final Locale locale) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateInstance(style, timeZone, locale));
   }

   public static FastDateFormat getTimeInstance(final int style) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getTimeInstance(style));
   }

   public static FastDateFormat getTimeInstance(final int style, final Locale locale) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getTimeInstance(style, locale));
   }

   public static FastDateFormat getTimeInstance(final int style, final TimeZone timeZone) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getTimeInstance(style, timeZone));
   }

   public static FastDateFormat getTimeInstance(final int style, final TimeZone timeZone, final Locale locale) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getTimeInstance(style, timeZone, locale));
   }

   public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(dateStyle, timeStyle));
   }

   public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle, final Locale locale) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(dateStyle, timeStyle, locale));
   }

   public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle, final TimeZone timeZone) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(dateStyle, timeStyle, timeZone));
   }

   public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle, final TimeZone timeZone, final Locale locale) {
      return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(dateStyle, timeStyle, timeZone, locale));
   }

   private FastDateFormat(final org.apache.logging.log4j.core.time.internal.format.FastDateFormat formatter) {
      this.formatter = formatter;
   }

   protected FastDateFormat(final String pattern, final TimeZone timeZone, final Locale locale) {
      this.formatter = org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern, timeZone, locale);
   }

   protected FastDateFormat(final String pattern, final TimeZone timeZone, final Locale locale, final Date centuryStart) {
      this.formatter = org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(pattern, timeZone, locale, centuryStart);
   }

   @Override
   public StringBuilder format(final Object obj, final StringBuilder toAppendTo, final FieldPosition pos) {
      return this.formatter.format(obj, toAppendTo, pos);
   }

   @Override
   public String format(final long millis) {
      return this.formatter.format(millis);
   }

   @Override
   public String format(final Date date) {
      return this.formatter.format(date);
   }

   @Override
   public String format(final Calendar calendar) {
      return this.formatter.format(calendar);
   }

   @Override
   public <B extends Appendable> B format(final long millis, final B buf) {
      return this.formatter.format(millis, buf);
   }

   @Override
   public <B extends Appendable> B format(final Date date, final B buf) {
      return this.formatter.format(date, buf);
   }

   @Override
   public <B extends Appendable> B format(final Calendar calendar, final B buf) {
      return this.formatter.format(calendar, buf);
   }

   @Override
   public Date parse(final String source) throws ParseException {
      return this.formatter.parse(source);
   }

   @Override
   public Date parse(final String source, final ParsePosition pos) {
      return this.formatter.parse(source, pos);
   }

   @Override
   public boolean parse(final String source, final ParsePosition pos, final Calendar calendar) {
      return this.formatter.parse(source, pos, calendar);
   }

   @Override
   public Object parseObject(final String source, final ParsePosition pos) {
      return this.formatter.parseObject(source, pos);
   }

   @Override
   public String getPattern() {
      return this.formatter.getPattern();
   }

   @Override
   public TimeZone getTimeZone() {
      return this.formatter.getTimeZone();
   }

   @Override
   public Locale getLocale() {
      return this.formatter.getLocale();
   }

   public int getMaxLengthEstimate() {
      return this.formatter.getMaxLengthEstimate();
   }

   @Override
   public boolean equals(final Object obj) {
      if (!(obj instanceof FastDateFormat)) {
         return false;
      }

      FastDateFormat other = (FastDateFormat)obj;
      return this.formatter.equals(other.formatter);
   }

   @Override
   public int hashCode() {
      return this.formatter.hashCode();
   }

   @Override
   public String toString() {
      return this.formatter.toString();
   }
}
