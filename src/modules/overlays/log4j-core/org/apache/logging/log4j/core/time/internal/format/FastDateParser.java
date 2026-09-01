package org.apache.logging.log4j.core.time.internal.format;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastDateParser implements DateParser {
   static final Locale JAPANESE_IMPERIAL = new Locale("ja", "JP", "JP");
   private final String pattern;
   private final TimeZone timeZone;
   private final Locale locale;
   private final int century;
   private final int startYear;
   private transient List<FastDateParser.StrategyAndWidth> patterns;
   private static final Comparator<String> LONGER_FIRST_LOWERCASE = (left, right) -> right.compareTo(left);
   private static final ConcurrentMap<Locale, FastDateParser.Strategy>[] caches = new ConcurrentMap[17];
   private static final FastDateParser.Strategy ABBREVIATED_YEAR_STRATEGY = new FastDateParser.NumberStrategy(1) {
      @Override
      int modify(final FastDateParser parser, final int iValue) {
         return iValue < 100 ? parser.adjustYear(iValue) : iValue;
      }
   };
   private static final FastDateParser.Strategy NUMBER_MONTH_STRATEGY = new FastDateParser.NumberStrategy(2) {
      @Override
      int modify(final FastDateParser parser, final int iValue) {
         return iValue - 1;
      }
   };
   private static final FastDateParser.Strategy LITERAL_YEAR_STRATEGY = new FastDateParser.NumberStrategy(1);
   private static final FastDateParser.Strategy WEEK_OF_YEAR_STRATEGY = new FastDateParser.NumberStrategy(3);
   private static final FastDateParser.Strategy WEEK_OF_MONTH_STRATEGY = new FastDateParser.NumberStrategy(4);
   private static final FastDateParser.Strategy DAY_OF_YEAR_STRATEGY = new FastDateParser.NumberStrategy(6);
   private static final FastDateParser.Strategy DAY_OF_MONTH_STRATEGY = new FastDateParser.NumberStrategy(5);
   private static final FastDateParser.Strategy DAY_OF_WEEK_STRATEGY = new FastDateParser.NumberStrategy(7) {
      @Override
      int modify(final FastDateParser parser, final int iValue) {
         return iValue != 7 ? iValue + 1 : 1;
      }
   };
   private static final FastDateParser.Strategy DAY_OF_WEEK_IN_MONTH_STRATEGY = new FastDateParser.NumberStrategy(8);
   private static final FastDateParser.Strategy HOUR_OF_DAY_STRATEGY = new FastDateParser.NumberStrategy(11);
   private static final FastDateParser.Strategy HOUR24_OF_DAY_STRATEGY = new FastDateParser.NumberStrategy(11) {
      @Override
      int modify(final FastDateParser parser, final int iValue) {
         return iValue == 24 ? 0 : iValue;
      }
   };
   private static final FastDateParser.Strategy HOUR12_STRATEGY = new FastDateParser.NumberStrategy(10) {
      @Override
      int modify(final FastDateParser parser, final int iValue) {
         return iValue == 12 ? 0 : iValue;
      }
   };
   private static final FastDateParser.Strategy HOUR_STRATEGY = new FastDateParser.NumberStrategy(10);
   private static final FastDateParser.Strategy MINUTE_STRATEGY = new FastDateParser.NumberStrategy(12);
   private static final FastDateParser.Strategy SECOND_STRATEGY = new FastDateParser.NumberStrategy(13);
   private static final FastDateParser.Strategy MILLISECOND_STRATEGY = new FastDateParser.NumberStrategy(14);

   protected FastDateParser(final String pattern, final TimeZone timeZone, final Locale locale) {
      this(pattern, timeZone, locale, null);
   }

   protected FastDateParser(final String pattern, final TimeZone timeZone, final Locale locale, final Date centuryStart) {
      this.pattern = pattern;
      this.timeZone = timeZone;
      this.locale = locale;
      Calendar definingCalendar = Calendar.getInstance(timeZone, locale);
      int centuryStartYear;
      if (centuryStart != null) {
         definingCalendar.setTime(centuryStart);
         centuryStartYear = definingCalendar.get(1);
      } else if (locale.equals(JAPANESE_IMPERIAL)) {
         centuryStartYear = 0;
      } else {
         definingCalendar.setTime(new Date());
         centuryStartYear = definingCalendar.get(1) - 80;
      }

      this.century = centuryStartYear / 100 * 100;
      this.startYear = centuryStartYear - this.century;
      this.init(definingCalendar);
   }

   private void init(final Calendar definingCalendar) {
      this.patterns = new ArrayList<>();
      FastDateParser.StrategyParser fm = new FastDateParser.StrategyParser(definingCalendar);

      while (true) {
         FastDateParser.StrategyAndWidth field = fm.getNextStrategy();
         if (field == null) {
            return;
         }

         this.patterns.add(field);
      }
   }

   private static boolean isFormatLetter(final char c) {
      return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z';
   }

   @Override
   public String getPattern() {
      return this.pattern;
   }

   @Override
   public TimeZone getTimeZone() {
      return this.timeZone;
   }

   @Override
   public Locale getLocale() {
      return this.locale;
   }

   @Override
   public boolean equals(final Object obj) {
      if (!(obj instanceof FastDateParser)) {
         return false;
      }

      FastDateParser other = (FastDateParser)obj;
      return this.pattern.equals(other.pattern) && this.timeZone.equals(other.timeZone) && this.locale.equals(other.locale);
   }

   @Override
   public int hashCode() {
      return this.pattern.hashCode() + 13 * (this.timeZone.hashCode() + 13 * this.locale.hashCode());
   }

   @Override
   public String toString() {
      return "FastDateParser[" + this.pattern + "," + this.locale + "," + this.timeZone.getID() + "]";
   }

   @Override
   public Object parseObject(final String source) throws ParseException {
      return this.parse(source);
   }

   @Override
   public Date parse(final String source) throws ParseException {
      ParsePosition pp = new ParsePosition(0);
      Date date = this.parse(source, pp);
      if (date == null) {
         if (this.locale.equals(JAPANESE_IMPERIAL)) {
            throw new ParseException(
               "(The " + this.locale + " locale does not support dates before 1868 AD)\nUnparseable date: \"" + source, pp.getErrorIndex()
            );
         } else {
            throw new ParseException("Unparseable date: " + source, pp.getErrorIndex());
         }
      } else {
         return date;
      }
   }

   @Override
   public Object parseObject(final String source, final ParsePosition pos) {
      return this.parse(source, pos);
   }

   @Override
   public Date parse(final String source, final ParsePosition pos) {
      Calendar cal = Calendar.getInstance(this.timeZone, this.locale);
      cal.clear();
      return this.parse(source, pos, cal) ? cal.getTime() : null;
   }

   @Override
   public boolean parse(final String source, final ParsePosition pos, final Calendar calendar) {
      ListIterator<FastDateParser.StrategyAndWidth> lt = this.patterns.listIterator();

      while (lt.hasNext()) {
         FastDateParser.StrategyAndWidth strategyAndWidth = lt.next();
         int maxWidth = strategyAndWidth.getMaxWidth(lt);
         if (!strategyAndWidth.strategy.parse(this, calendar, source, pos, maxWidth)) {
            return false;
         }
      }

      return true;
   }

   private static StringBuilder simpleQuote(final StringBuilder sb, final String value) {
      for (int i = 0; i < value.length(); i++) {
         char c = value.charAt(i);
         switch (c) {
            case '$':
            case '(':
            case ')':
            case '*':
            case '+':
            case '.':
            case '?':
            case '[':
            case '\\':
            case '^':
            case '{':
            case '|':
               sb.append('\\');
            default:
               sb.append(c);
         }
      }

      return sb;
   }

   private static Map<String, Integer> appendDisplayNames(final Calendar cal, final Locale locale, final int field, final StringBuilder regex) {
      Map<String, Integer> values = new HashMap<>();
      Map<String, Integer> displayNames = cal.getDisplayNames(field, 0, locale);
      TreeSet<String> sorted = new TreeSet<>(LONGER_FIRST_LOWERCASE);

      for (Entry<String, Integer> displayName : displayNames.entrySet()) {
         String key = displayName.getKey().toLowerCase(locale);
         if (sorted.add(key)) {
            values.put(key, displayName.getValue());
         }
      }

      for (String symbol : sorted) {
         simpleQuote(regex, symbol).append('|');
      }

      return values;
   }

   private int adjustYear(final int twoDigitYear) {
      int trial = this.century + twoDigitYear;
      return twoDigitYear >= this.startYear ? trial : trial + 100;
   }

   private FastDateParser.Strategy getStrategy(final char f, final int width, final Calendar definingCalendar) {
      switch (f) {
         case 'D':
            return DAY_OF_YEAR_STRATEGY;
         case 'E':
            return this.getLocaleSpecificStrategy(7, definingCalendar);
         case 'F':
            return DAY_OF_WEEK_IN_MONTH_STRATEGY;
         case 'G':
            return this.getLocaleSpecificStrategy(0, definingCalendar);
         case 'H':
            return HOUR_OF_DAY_STRATEGY;
         case 'I':
         case 'J':
         case 'L':
         case 'N':
         case 'O':
         case 'P':
         case 'Q':
         case 'R':
         case 'T':
         case 'U':
         case 'V':
         case '[':
         case '\\':
         case ']':
         case '^':
         case '_':
         case '`':
         case 'b':
         case 'c':
         case 'e':
         case 'f':
         case 'g':
         case 'i':
         case 'j':
         case 'l':
         case 'n':
         case 'o':
         case 'p':
         case 'q':
         case 'r':
         case 't':
         case 'v':
         case 'x':
         default:
            throw new IllegalArgumentException("Format '" + f + "' not supported");
         case 'K':
            return HOUR_STRATEGY;
         case 'M':
            return width >= 3 ? this.getLocaleSpecificStrategy(2, definingCalendar) : NUMBER_MONTH_STRATEGY;
         case 'S':
            return MILLISECOND_STRATEGY;
         case 'W':
            return WEEK_OF_MONTH_STRATEGY;
         case 'X':
            return FastDateParser.ISO8601TimeZoneStrategy.getStrategy(width);
         case 'Y':
         case 'y':
            return width > 2 ? LITERAL_YEAR_STRATEGY : ABBREVIATED_YEAR_STRATEGY;
         case 'Z':
            if (width == 2) {
               return FastDateParser.ISO8601TimeZoneStrategy.ISO_8601_3_STRATEGY;
            }
         case 'z':
            return this.getLocaleSpecificStrategy(15, definingCalendar);
         case 'a':
            return this.getLocaleSpecificStrategy(9, definingCalendar);
         case 'd':
            return DAY_OF_MONTH_STRATEGY;
         case 'h':
            return HOUR12_STRATEGY;
         case 'k':
            return HOUR24_OF_DAY_STRATEGY;
         case 'm':
            return MINUTE_STRATEGY;
         case 's':
            return SECOND_STRATEGY;
         case 'u':
            return DAY_OF_WEEK_STRATEGY;
         case 'w':
            return WEEK_OF_YEAR_STRATEGY;
      }
   }

   private static ConcurrentMap<Locale, FastDateParser.Strategy> getCache(final int field) {
      synchronized (caches) {
         if (caches[field] == null) {
            caches[field] = new ConcurrentHashMap<>(3);
         }

         return caches[field];
      }
   }

   private FastDateParser.Strategy getLocaleSpecificStrategy(final int field, final Calendar definingCalendar) {
      ConcurrentMap<Locale, FastDateParser.Strategy> cache = getCache(field);
      FastDateParser.Strategy strategy = cache.get(this.locale);
      if (strategy == null) {
         strategy = field == 15
            ? new FastDateParser.TimeZoneStrategy(this.locale)
            : new FastDateParser.CaseInsensitiveTextStrategy(field, definingCalendar, this.locale);
         FastDateParser.Strategy inCache = cache.putIfAbsent(this.locale, strategy);
         if (inCache != null) {
            return inCache;
         }
      }

      return strategy;
   }

   private static class CaseInsensitiveTextStrategy extends FastDateParser.PatternStrategy {
      private final int field;
      final Locale locale;
      private final Map<String, Integer> lKeyValues;

      CaseInsensitiveTextStrategy(final int field, final Calendar definingCalendar, final Locale locale) {
         this.field = field;
         this.locale = locale;
         StringBuilder regex = new StringBuilder();
         regex.append("((?iu)");
         this.lKeyValues = FastDateParser.appendDisplayNames(definingCalendar, locale, field, regex);
         regex.setLength(regex.length() - 1);
         regex.append(")");
         this.createPattern(regex);
      }

      @Override
      void setCalendar(final FastDateParser parser, final Calendar cal, final String value) {
         Integer iVal = this.lKeyValues.get(value.toLowerCase(this.locale));
         cal.set(this.field, iVal);
      }
   }

   private static class CopyQuotedStrategy extends FastDateParser.Strategy {
      private final String formatField;

      CopyQuotedStrategy(final String formatField) {
         this.formatField = formatField;
      }

      @Override
      boolean isNumber() {
         return false;
      }

      @Override
      boolean parse(final FastDateParser parser, final Calendar calendar, final String source, final ParsePosition pos, final int maxWidth) {
         for (int idx = 0; idx < this.formatField.length(); idx++) {
            int sIdx = idx + pos.getIndex();
            if (sIdx == source.length()) {
               pos.setErrorIndex(sIdx);
               return false;
            }

            if (this.formatField.charAt(idx) != source.charAt(sIdx)) {
               pos.setErrorIndex(sIdx);
               return false;
            }
         }

         pos.setIndex(this.formatField.length() + pos.getIndex());
         return true;
      }
   }

   private static class ISO8601TimeZoneStrategy extends FastDateParser.PatternStrategy {
      private static final FastDateParser.Strategy ISO_8601_1_STRATEGY = new FastDateParser.ISO8601TimeZoneStrategy("(Z|(?:[+-]\\d{2}))");
      private static final FastDateParser.Strategy ISO_8601_2_STRATEGY = new FastDateParser.ISO8601TimeZoneStrategy("(Z|(?:[+-]\\d{2}\\d{2}))");
      private static final FastDateParser.Strategy ISO_8601_3_STRATEGY = new FastDateParser.ISO8601TimeZoneStrategy("(Z|(?:[+-]\\d{2}(?::)\\d{2}))");

      ISO8601TimeZoneStrategy(final String pattern) {
         this.createPattern(pattern);
      }

      @Override
      void setCalendar(final FastDateParser parser, final Calendar cal, final String value) {
         if (value.equals("Z")) {
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
         } else {
            cal.setTimeZone(TimeZone.getTimeZone("GMT" + value));
         }
      }

      static FastDateParser.Strategy getStrategy(final int tokenLen) {
         switch (tokenLen) {
            case 1:
               return ISO_8601_1_STRATEGY;
            case 2:
               return ISO_8601_2_STRATEGY;
            case 3:
               return ISO_8601_3_STRATEGY;
            default:
               throw new IllegalArgumentException("invalid number of X");
         }
      }
   }

   private static class NumberStrategy extends FastDateParser.Strategy {
      private final int field;

      NumberStrategy(final int field) {
         this.field = field;
      }

      @Override
      boolean isNumber() {
         return true;
      }

      @Override
      boolean parse(final FastDateParser parser, final Calendar calendar, final String source, final ParsePosition pos, final int maxWidth) {
         int idx = pos.getIndex();
         int last = source.length();
         if (maxWidth != 0) {
            int end = idx + maxWidth;
            if (last > end) {
               last = end;
            }
         } else {
            while (true) {
               if (idx < last) {
                  char c = source.charAt(idx);
                  if (Character.isWhitespace(c)) {
                     idx++;
                     continue;
                  }
               }

               pos.setIndex(idx);
               break;
            }
         }

         while (idx < last) {
            char c = source.charAt(idx);
            if (!Character.isDigit(c)) {
               break;
            }

            idx++;
         }

         if (pos.getIndex() == idx) {
            pos.setErrorIndex(idx);
            return false;
         } else {
            int value = Integer.parseInt(source.substring(pos.getIndex(), idx));
            pos.setIndex(idx);
            calendar.set(this.field, this.modify(parser, value));
            return true;
         }
      }

      int modify(final FastDateParser parser, final int iValue) {
         return iValue;
      }
   }

   private abstract static class PatternStrategy extends FastDateParser.Strategy {
      private Pattern pattern;

      void createPattern(final StringBuilder regex) {
         this.createPattern(regex.toString());
      }

      void createPattern(final String regex) {
         this.pattern = Pattern.compile(regex);
      }

      @Override
      boolean isNumber() {
         return false;
      }

      @Override
      boolean parse(final FastDateParser parser, final Calendar calendar, final String source, final ParsePosition pos, final int maxWidth) {
         Matcher matcher = this.pattern.matcher(source.substring(pos.getIndex()));
         if (!matcher.lookingAt()) {
            pos.setErrorIndex(pos.getIndex());
            return false;
         } else {
            pos.setIndex(pos.getIndex() + matcher.end(1));
            this.setCalendar(parser, calendar, matcher.group(1));
            return true;
         }
      }

      abstract void setCalendar(FastDateParser parser, Calendar cal, String value);
   }

   private abstract static class Strategy {
      boolean isNumber() {
         return false;
      }

      abstract boolean parse(FastDateParser parser, Calendar calendar, String source, ParsePosition pos, int maxWidth);
   }

   private static class StrategyAndWidth {
      final FastDateParser.Strategy strategy;
      final int width;

      StrategyAndWidth(final FastDateParser.Strategy strategy, final int width) {
         this.strategy = strategy;
         this.width = width;
      }

      int getMaxWidth(final ListIterator<FastDateParser.StrategyAndWidth> lt) {
         if (this.strategy.isNumber() && lt.hasNext()) {
            FastDateParser.Strategy nextStrategy = lt.next().strategy;
            lt.previous();
            return nextStrategy.isNumber() ? this.width : 0;
         } else {
            return 0;
         }
      }
   }

   private class StrategyParser {
      private final Calendar definingCalendar;
      private int currentIdx;

      StrategyParser(final Calendar definingCalendar) {
         this.definingCalendar = definingCalendar;
      }

      FastDateParser.StrategyAndWidth getNextStrategy() {
         if (this.currentIdx >= FastDateParser.this.pattern.length()) {
            return null;
         }

         char c = FastDateParser.this.pattern.charAt(this.currentIdx);
         return FastDateParser.isFormatLetter(c) ? this.letterPattern(c) : this.literal();
      }

      private FastDateParser.StrategyAndWidth letterPattern(final char c) {
         int begin = this.currentIdx;

         while (++this.currentIdx < FastDateParser.this.pattern.length() && FastDateParser.this.pattern.charAt(this.currentIdx) == c) {
         }

         int width = this.currentIdx - begin;
         return new FastDateParser.StrategyAndWidth(FastDateParser.this.getStrategy(c, width, this.definingCalendar), width);
      }

      private FastDateParser.StrategyAndWidth literal() {
         boolean activeQuote = false;
         StringBuilder sb = new StringBuilder();

         while (this.currentIdx < FastDateParser.this.pattern.length()) {
            char c = FastDateParser.this.pattern.charAt(this.currentIdx);
            if (!activeQuote && FastDateParser.isFormatLetter(c)) {
               break;
            }

            if (c == '\'' && (++this.currentIdx == FastDateParser.this.pattern.length() || FastDateParser.this.pattern.charAt(this.currentIdx) != '\'')) {
               activeQuote = !activeQuote;
            } else {
               this.currentIdx++;
               sb.append(c);
            }
         }

         if (activeQuote) {
            throw new IllegalArgumentException("Unterminated quote");
         }

         String formatField = sb.toString();
         return new FastDateParser.StrategyAndWidth(new FastDateParser.CopyQuotedStrategy(formatField), formatField.length());
      }
   }

   static class TimeZoneStrategy extends FastDateParser.PatternStrategy {
      private static final String RFC_822_TIME_ZONE = "[+-]\\d{4}";
      private static final String GMT_OPTION = "GMT[+-]\\d{1,2}:\\d{2}";
      private final Locale locale;
      private final Map<String, FastDateParser.TimeZoneStrategy.TzInfo> tzNames = new HashMap<>();
      private static final int ID = 0;

      TimeZoneStrategy(final Locale locale) {
         this.locale = locale;
         StringBuilder sb = new StringBuilder();
         sb.append("((?iu)[+-]\\d{4}|GMT[+-]\\d{1,2}:\\d{2}");
         Set<String> sorted = new TreeSet<>(FastDateParser.LONGER_FIRST_LOWERCASE);
         String[][] zones = DateFormatSymbols.getInstance(locale).getZoneStrings();

         for (String[] zoneNames : zones) {
            String tzId = zoneNames[0];
            if (!tzId.equalsIgnoreCase("GMT")) {
               TimeZone tz = TimeZone.getTimeZone(tzId);
               FastDateParser.TimeZoneStrategy.TzInfo standard = new FastDateParser.TimeZoneStrategy.TzInfo(tz, false);
               FastDateParser.TimeZoneStrategy.TzInfo tzInfo = standard;

               for (int i = 1; i < zoneNames.length; i++) {
                  switch (i) {
                     case 3:
                        tzInfo = new FastDateParser.TimeZoneStrategy.TzInfo(tz, true);
                        break;
                     case 5:
                        tzInfo = standard;
                  }

                  if (zoneNames[i] != null) {
                     String key = zoneNames[i].toLowerCase(locale);
                     if (sorted.add(key)) {
                        this.tzNames.put(key, tzInfo);
                     }
                  }
               }
            }
         }

         for (String zoneName : sorted) {
            FastDateParser.simpleQuote(sb.append('|'), zoneName);
         }

         sb.append(")");
         this.createPattern(sb);
      }

      @Override
      void setCalendar(final FastDateParser parser, final Calendar cal, final String value) {
         if (value.charAt(0) == '+' || value.charAt(0) == '-') {
            TimeZone tz = TimeZone.getTimeZone("GMT" + value);
            cal.setTimeZone(tz);
         } else if (value.regionMatches(true, 0, "GMT", 0, 3)) {
            TimeZone tz = TimeZone.getTimeZone(value.toUpperCase());
            cal.setTimeZone(tz);
         } else {
            FastDateParser.TimeZoneStrategy.TzInfo tzInfo = this.tzNames.get(value.toLowerCase(this.locale));
            cal.set(16, tzInfo.dstOffset);
            cal.set(15, tzInfo.zone.getRawOffset());
         }
      }

      private static class TzInfo {
         TimeZone zone;
         int dstOffset;

         TzInfo(final TimeZone tz, final boolean useDst) {
            this.zone = tz;
            this.dstOffset = useDst ? tz.getDSTSavings() : 0;
         }
      }
   }
}
