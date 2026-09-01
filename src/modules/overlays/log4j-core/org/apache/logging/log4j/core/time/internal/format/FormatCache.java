package org.apache.logging.log4j.core.time.internal.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

abstract class FormatCache<F extends Format> {
   static final int NONE = -1;
   private final ConcurrentMap<FormatCache.MultipartKey, F> cInstanceCache = new ConcurrentHashMap<>(7);
   private static final ConcurrentMap<FormatCache.MultipartKey, String> cDateTimeInstanceCache = new ConcurrentHashMap<>(7);

   public F getInstance() {
      return this.getDateTimeInstance(3, 3, TimeZone.getDefault(), Locale.getDefault());
   }

   public F getInstance(final String pattern, TimeZone timeZone, Locale locale) {
      if (pattern == null) {
         throw new NullPointerException("pattern must not be null");
      }

      if (timeZone == null) {
         timeZone = TimeZone.getDefault();
      }

      if (locale == null) {
         locale = Locale.getDefault();
      }

      FormatCache.MultipartKey key = new FormatCache.MultipartKey(pattern, timeZone, locale);
      F format = this.cInstanceCache.get(key);
      if (format == null) {
         format = this.createInstance(pattern, timeZone, locale);
         F previousValue = this.cInstanceCache.putIfAbsent(key, format);
         if (previousValue != null) {
            format = previousValue;
         }
      }

      return format;
   }

   protected abstract F createInstance(String pattern, TimeZone timeZone, Locale locale);

   private F getDateTimeInstance(final Integer dateStyle, final Integer timeStyle, final TimeZone timeZone, Locale locale) {
      if (locale == null) {
         locale = Locale.getDefault();
      }

      String pattern = getPatternForStyle(dateStyle, timeStyle, locale);
      return this.getInstance(pattern, timeZone, locale);
   }

   F getDateTimeInstance(final int dateStyle, final int timeStyle, final TimeZone timeZone, final Locale locale) {
      return this.getDateTimeInstance(dateStyle, Integer.valueOf(timeStyle), timeZone, locale);
   }

   F getDateInstance(final int dateStyle, final TimeZone timeZone, final Locale locale) {
      return this.getDateTimeInstance(dateStyle, null, timeZone, locale);
   }

   F getTimeInstance(final int timeStyle, final TimeZone timeZone, final Locale locale) {
      return this.getDateTimeInstance(null, timeStyle, timeZone, locale);
   }

   static String getPatternForStyle(final Integer dateStyle, final Integer timeStyle, final Locale locale) {
      FormatCache.MultipartKey key = new FormatCache.MultipartKey(dateStyle, timeStyle, locale);
      String pattern = cDateTimeInstanceCache.get(key);
      if (pattern == null) {
         try {
            DateFormat formatter;
            if (dateStyle == null) {
               formatter = DateFormat.getTimeInstance(timeStyle, locale);
            } else if (timeStyle == null) {
               formatter = DateFormat.getDateInstance(dateStyle, locale);
            } else {
               formatter = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
            }

            pattern = ((SimpleDateFormat)formatter).toPattern();
            String previous = cDateTimeInstanceCache.putIfAbsent(key, pattern);
            if (previous != null) {
               pattern = previous;
            }
         } catch (ClassCastException ex) {
            throw new IllegalArgumentException("No date time pattern for locale: " + locale);
         }
      }

      return pattern;
   }

   private static class MultipartKey {
      private final Object[] keys;
      private int hashCode;

      public MultipartKey(final Object... keys) {
         this.keys = keys;
      }

      @Override
      public boolean equals(final Object obj) {
         return Arrays.equals(this.keys, ((FormatCache.MultipartKey)obj).keys);
      }

      @Override
      public int hashCode() {
         if (this.hashCode == 0) {
            int rc = 0;

            for (Object key : this.keys) {
               if (key != null) {
                  rc = rc * 7 + key.hashCode();
               }
            }

            this.hashCode = rc;
         }

         return this.hashCode;
      }
   }
}
