package com.thoughtworks.xstream.core.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ThreadSafeSimpleDateFormat {
   private final String formatString;
   private final Pool pool;
   private final TimeZone timeZone;

   public ThreadSafeSimpleDateFormat(String format, TimeZone timeZone, int initialPoolSize, int maxPoolSize, boolean lenient) {
      this(format, timeZone, Locale.ENGLISH, initialPoolSize, maxPoolSize, lenient);
   }

   public ThreadSafeSimpleDateFormat(String format, TimeZone timeZone, Locale locale, int initialPoolSize, int maxPoolSize, boolean lenient) {
      this.formatString = format;
      this.timeZone = timeZone;
      this.pool = new Pool(initialPoolSize, maxPoolSize, new ThreadSafeSimpleDateFormat$1(this, locale, lenient));
   }

   public String format(Date date) {
      DateFormat format = this.fetchFromPool();

      try {
         return format.format(date);
      } finally {
         this.pool.putInPool(format);
      }
   }

   public Date parse(String date) throws ParseException {
      DateFormat format = this.fetchFromPool();

      try {
         return format.parse(date);
      } finally {
         this.pool.putInPool(format);
      }
   }

   private DateFormat fetchFromPool() {
      DateFormat format = (DateFormat)this.pool.fetchFromPool();
      TimeZone tz = this.timeZone != null ? this.timeZone : TimeZone.getDefault();
      if (!tz.equals(format.getTimeZone())) {
         format.setTimeZone(tz);
      }

      return format;
   }

   public String toString() {
      return this.formatString;
   }
}
