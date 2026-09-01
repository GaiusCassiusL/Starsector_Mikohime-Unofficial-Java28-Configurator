package org.apache.logging.log4j.core.time.internal.format;

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.core.util.Throwables;

public class FastDatePrinter implements DatePrinter {
   public static final int FULL = 0;
   public static final int LONG = 1;
   public static final int MEDIUM = 2;
   public static final int SHORT = 3;
   private final String mPattern;
   private final TimeZone mTimeZone;
   private final Locale mLocale;
   private transient FastDatePrinter.Rule[] mRules;
   private transient int mMaxLengthEstimate;
   private static final int MAX_DIGITS = 10;
   private static final ConcurrentMap<FastDatePrinter.TimeZoneDisplayKey, String> cTimeZoneDisplayCache = new ConcurrentHashMap<>(7);

   protected FastDatePrinter(final String pattern, final TimeZone timeZone, final Locale locale) {
      this.mPattern = pattern;
      this.mTimeZone = timeZone;
      this.mLocale = locale;
      this.init();
   }

   private void init() {
      List<FastDatePrinter.Rule> rulesList = this.parsePattern();
      this.mRules = rulesList.toArray(FastDatePrinter.Rule.EMPTY_ARRAY);
      int len = 0;
      int i = this.mRules.length;

      while (--i >= 0) {
         len += this.mRules[i].estimateLength();
      }

      this.mMaxLengthEstimate = len;
   }

   protected List<FastDatePrinter.Rule> parsePattern() {
      DateFormatSymbols symbols = new DateFormatSymbols(this.mLocale);
      List<FastDatePrinter.Rule> rules = new ArrayList<>();
      String[] ERAs = symbols.getEras();
      String[] months = symbols.getMonths();
      String[] shortMonths = symbols.getShortMonths();
      String[] weekdays = symbols.getWeekdays();
      String[] shortWeekdays = symbols.getShortWeekdays();
      String[] AmPmStrings = symbols.getAmPmStrings();
      int length = this.mPattern.length();
      int[] indexRef = new int[1];

      for (int i = 0; i < length; i++) {
         indexRef[0] = i;
         String token = this.parseToken(this.mPattern, indexRef);
         i = indexRef[0];
         int tokenLen = token.length();
         if (tokenLen == 0) {
            break;
         }

         char c = token.charAt(0);
         FastDatePrinter.Rule rule;
         switch (c) {
            case '\'':
               String sub = token.substring(1);
               if (sub.length() == 1) {
                  rule = new FastDatePrinter.CharacterLiteral(sub.charAt(0));
               } else {
                  rule = new FastDatePrinter.StringLiteral(sub);
               }
               break;
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case '-':
            case '.':
            case '/':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case 'A':
            case 'B':
            case 'C':
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
               throw new IllegalArgumentException("Illegal pattern component: " + token);
            case 'D':
               rule = this.selectNumberRule(6, tokenLen);
               break;
            case 'E':
               rule = new FastDatePrinter.TextField(7, tokenLen < 4 ? shortWeekdays : weekdays);
               break;
            case 'F':
               rule = this.selectNumberRule(8, tokenLen);
               break;
            case 'G':
               rule = new FastDatePrinter.TextField(0, ERAs);
               break;
            case 'H':
               rule = this.selectNumberRule(11, tokenLen);
               break;
            case 'K':
               rule = this.selectNumberRule(10, tokenLen);
               break;
            case 'M':
               if (tokenLen >= 4) {
                  rule = new FastDatePrinter.TextField(2, months);
               } else if (tokenLen == 3) {
                  rule = new FastDatePrinter.TextField(2, shortMonths);
               } else if (tokenLen == 2) {
                  rule = FastDatePrinter.TwoDigitMonthField.INSTANCE;
               } else {
                  rule = FastDatePrinter.UnpaddedMonthField.INSTANCE;
               }
               break;
            case 'S':
               rule = this.selectNumberRule(14, tokenLen);
               break;
            case 'W':
               rule = this.selectNumberRule(4, tokenLen);
               break;
            case 'X':
               rule = FastDatePrinter.Iso8601_Rule.getRule(tokenLen);
               break;
            case 'Y':
            case 'y':
               if (tokenLen == 2) {
                  rule = FastDatePrinter.TwoDigitYearField.INSTANCE;
               } else {
                  rule = this.selectNumberRule(1, tokenLen < 4 ? 4 : tokenLen);
               }

               if (c == 'Y') {
                  rule = new FastDatePrinter.WeekYear((FastDatePrinter.NumberRule)rule);
               }
               break;
            case 'Z':
               if (tokenLen == 1) {
                  rule = FastDatePrinter.TimeZoneNumberRule.INSTANCE_NO_COLON;
               } else if (tokenLen == 2) {
                  rule = FastDatePrinter.Iso8601_Rule.ISO8601_HOURS_COLON_MINUTES;
               } else {
                  rule = FastDatePrinter.TimeZoneNumberRule.INSTANCE_COLON;
               }
               break;
            case 'a':
               rule = new FastDatePrinter.TextField(9, AmPmStrings);
               break;
            case 'd':
               rule = this.selectNumberRule(5, tokenLen);
               break;
            case 'h':
               rule = new FastDatePrinter.TwelveHourField(this.selectNumberRule(10, tokenLen));
               break;
            case 'k':
               rule = new FastDatePrinter.TwentyFourHourField(this.selectNumberRule(11, tokenLen));
               break;
            case 'm':
               rule = this.selectNumberRule(12, tokenLen);
               break;
            case 's':
               rule = this.selectNumberRule(13, tokenLen);
               break;
            case 'u':
               rule = new FastDatePrinter.DayInWeekField(this.selectNumberRule(7, tokenLen));
               break;
            case 'w':
               rule = this.selectNumberRule(3, tokenLen);
               break;
            case 'z':
               if (tokenLen >= 4) {
                  rule = new FastDatePrinter.TimeZoneNameRule(this.mTimeZone, this.mLocale, 1);
               } else {
                  rule = new FastDatePrinter.TimeZoneNameRule(this.mTimeZone, this.mLocale, 0);
               }
         }

         rules.add(rule);
      }

      return rules;
   }

   protected String parseToken(final String pattern, final int[] indexRef) {
      StringBuilder buf = new StringBuilder();
      int i = indexRef[0];
      int length = pattern.length();
      char c = pattern.charAt(i);
      if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z') {
         buf.append(c);

         while (i + 1 < length) {
            char peek = pattern.charAt(i + 1);
            if (peek != c) {
               break;
            }

            buf.append(c);
            i++;
         }
      } else {
         buf.append('\'');
         boolean inLiteral = false;

         while (i < length) {
            c = pattern.charAt(i);
            if (c == '\'') {
               if (i + 1 < length && pattern.charAt(i + 1) == '\'') {
                  i++;
                  buf.append(c);
               } else {
                  inLiteral = !inLiteral;
               }
            } else {
               if (!inLiteral && (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')) {
                  i--;
                  break;
               }

               buf.append(c);
            }

            i++;
         }
      }

      indexRef[0] = i;
      return buf.toString();
   }

   protected FastDatePrinter.NumberRule selectNumberRule(final int field, final int padding) {
      switch (padding) {
         case 1:
            return new FastDatePrinter.UnpaddedNumberField(field);
         case 2:
            return new FastDatePrinter.TwoDigitNumberField(field);
         default:
            return new FastDatePrinter.PaddedNumberField(field, padding);
      }
   }

   @Deprecated
   @Override
   public StringBuilder format(final Object obj, final StringBuilder toAppendTo, final FieldPosition pos) {
      if (obj instanceof Date) {
         return this.format((Date)obj, toAppendTo);
      } else if (obj instanceof Calendar) {
         return this.format((Calendar)obj, toAppendTo);
      } else if (obj instanceof Long) {
         return this.format((Long)obj, toAppendTo);
      } else {
         throw new IllegalArgumentException("Unknown class: " + (obj == null ? "<null>" : obj.getClass().getName()));
      }
   }

   String format(final Object obj) {
      if (obj instanceof Date) {
         return this.format((Date)obj);
      } else if (obj instanceof Calendar) {
         return this.format((Calendar)obj);
      } else if (obj instanceof Long) {
         return this.format(((Long)obj).longValue());
      } else {
         throw new IllegalArgumentException("Unknown class: " + (obj == null ? "<null>" : obj.getClass().getName()));
      }
   }

   @Override
   public String format(final long millis) {
      Calendar c = this.newCalendar();
      c.setTimeInMillis(millis);
      return this.applyRulesToString(c);
   }

   private String applyRulesToString(final Calendar c) {
      return this.applyRules(c, new StringBuilder(this.mMaxLengthEstimate)).toString();
   }

   private Calendar newCalendar() {
      return Calendar.getInstance(this.mTimeZone, this.mLocale);
   }

   @Override
   public String format(final Date date) {
      Calendar c = this.newCalendar();
      c.setTime(date);
      return this.applyRulesToString(c);
   }

   @Override
   public String format(final Calendar calendar) {
      return this.format(calendar, new StringBuilder(this.mMaxLengthEstimate)).toString();
   }

   @Override
   public <B extends Appendable> B format(final long millis, final B buf) {
      Calendar c = this.newCalendar();
      c.setTimeInMillis(millis);
      return this.applyRules(c, buf);
   }

   @Override
   public <B extends Appendable> B format(final Date date, final B buf) {
      Calendar c = this.newCalendar();
      c.setTime(date);
      return this.applyRules(c, buf);
   }

   @Override
   public <B extends Appendable> B format(Calendar calendar, final B buf) {
      if (!calendar.getTimeZone().equals(this.mTimeZone)) {
         calendar = (Calendar)calendar.clone();
         calendar.setTimeZone(this.mTimeZone);
      }

      return this.applyRules(calendar, buf);
   }

   private <B extends Appendable> B applyRules(final Calendar calendar, final B buf) {
      try {
         for (FastDatePrinter.Rule rule : this.mRules) {
            rule.appendTo(buf, calendar);
         }
      } catch (IOException ioe) {
         Throwables.rethrow(ioe);
      }

      return buf;
   }

   @Override
   public String getPattern() {
      return this.mPattern;
   }

   @Override
   public TimeZone getTimeZone() {
      return this.mTimeZone;
   }

   @Override
   public Locale getLocale() {
      return this.mLocale;
   }

   public int getMaxLengthEstimate() {
      return this.mMaxLengthEstimate;
   }

   @Override
   public boolean equals(final Object obj) {
      if (!(obj instanceof FastDatePrinter)) {
         return false;
      }

      FastDatePrinter other = (FastDatePrinter)obj;
      return this.mPattern.equals(other.mPattern) && this.mTimeZone.equals(other.mTimeZone) && this.mLocale.equals(other.mLocale);
   }

   @Override
   public int hashCode() {
      return this.mPattern.hashCode() + 13 * (this.mTimeZone.hashCode() + 13 * this.mLocale.hashCode());
   }

   @Override
   public String toString() {
      return "FastDatePrinter[" + this.mPattern + "," + this.mLocale + "," + this.mTimeZone.getID() + "]";
   }

   private static void appendDigits(final Appendable buffer, final int value) throws IOException {
      buffer.append((char)(value / 10 + 48));
      buffer.append((char)(value % 10 + 48));
   }

   private static void appendFullDigits(final Appendable buffer, int value, int minFieldWidth) throws IOException {
      if (value < 10000) {
         int nDigits = 4;
         if (value < 1000) {
            nDigits--;
            if (value < 100) {
               nDigits--;
               if (value < 10) {
                  nDigits--;
               }
            }
         }

         for (int i = minFieldWidth - nDigits; i > 0; i--) {
            buffer.append('0');
         }

         switch (nDigits) {
            case 4:
               buffer.append((char)(value / 1000 + 48));
               value %= 1000;
            case 3:
               if (value >= 100) {
                  buffer.append((char)(value / 100 + 48));
                  value %= 100;
               } else {
                  buffer.append('0');
               }
            case 2:
               if (value >= 10) {
                  buffer.append((char)(value / 10 + 48));
                  value %= 10;
               } else {
                  buffer.append('0');
               }
            case 1:
               buffer.append((char)(value + 48));
         }
      } else {
         char[] work = new char[10];
         int digit = 0;

         while (value != 0) {
            work[digit++] = (char)(value % 10 + 48);
            value /= 10;
         }

         while (digit < minFieldWidth) {
            buffer.append('0');
            minFieldWidth--;
         }

         while (--digit >= 0) {
            buffer.append(work[digit]);
         }
      }
   }

   static String getTimeZoneDisplay(final TimeZone tz, final boolean daylight, final int style, final Locale locale) {
      FastDatePrinter.TimeZoneDisplayKey key = new FastDatePrinter.TimeZoneDisplayKey(tz, daylight, style, locale);
      String value = cTimeZoneDisplayCache.get(key);
      if (value == null) {
         value = tz.getDisplayName(daylight, style, locale);
         String prior = cTimeZoneDisplayCache.putIfAbsent(key, value);
         if (prior != null) {
            value = prior;
         }
      }

      return value;
   }

   private static class CharacterLiteral implements FastDatePrinter.Rule {
      private final char mValue;

      CharacterLiteral(final char value) {
         this.mValue = value;
      }

      @Override
      public int estimateLength() {
         return 1;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         buffer.append(this.mValue);
      }
   }

   private static class DayInWeekField implements FastDatePrinter.NumberRule {
      private final FastDatePrinter.NumberRule mRule;

      DayInWeekField(final FastDatePrinter.NumberRule rule) {
         this.mRule = rule;
      }

      @Override
      public int estimateLength() {
         return this.mRule.estimateLength();
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         int value = calendar.get(7);
         this.mRule.appendTo(buffer, value != 1 ? value - 1 : 7);
      }

      @Override
      public void appendTo(final Appendable buffer, final int value) throws IOException {
         this.mRule.appendTo(buffer, value);
      }
   }

   private static class Iso8601_Rule implements FastDatePrinter.Rule {
      static final FastDatePrinter.Iso8601_Rule ISO8601_HOURS = new FastDatePrinter.Iso8601_Rule(3);
      static final FastDatePrinter.Iso8601_Rule ISO8601_HOURS_MINUTES = new FastDatePrinter.Iso8601_Rule(5);
      static final FastDatePrinter.Iso8601_Rule ISO8601_HOURS_COLON_MINUTES = new FastDatePrinter.Iso8601_Rule(6);
      final int length;

      static FastDatePrinter.Iso8601_Rule getRule(final int tokenLen) {
         switch (tokenLen) {
            case 1:
               return ISO8601_HOURS;
            case 2:
               return ISO8601_HOURS_MINUTES;
            case 3:
               return ISO8601_HOURS_COLON_MINUTES;
            default:
               throw new IllegalArgumentException("invalid number of X");
         }
      }

      Iso8601_Rule(final int length) {
         this.length = length;
      }

      @Override
      public int estimateLength() {
         return this.length;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         int offset = calendar.get(15) + calendar.get(16);
         if (offset == 0) {
            buffer.append("Z");
         } else {
            if (offset < 0) {
               buffer.append('-');
               offset = -offset;
            } else {
               buffer.append('+');
            }

            int hours = offset / 3600000;
            FastDatePrinter.appendDigits(buffer, hours);
            if (this.length >= 5) {
               if (this.length == 6) {
                  buffer.append(':');
               }

               int minutes = offset / 60000 - 60 * hours;
               FastDatePrinter.appendDigits(buffer, minutes);
            }
         }
      }
   }

   private interface NumberRule extends FastDatePrinter.Rule {
      void appendTo(Appendable buffer, int value) throws IOException;
   }

   private static class PaddedNumberField implements FastDatePrinter.NumberRule {
      private final int mField;
      private final int mSize;

      PaddedNumberField(final int field, final int size) {
         if (size < 3) {
            throw new IllegalArgumentException();
         }

         this.mField = field;
         this.mSize = size;
      }

      @Override
      public int estimateLength() {
         return this.mSize;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         this.appendTo(buffer, calendar.get(this.mField));
      }

      @Override
      public final void appendTo(final Appendable buffer, final int value) throws IOException {
         FastDatePrinter.appendFullDigits(buffer, value, this.mSize);
      }
   }

   private interface Rule {
      FastDatePrinter.Rule[] EMPTY_ARRAY = new FastDatePrinter.Rule[0];

      int estimateLength();

      void appendTo(Appendable buf, Calendar calendar) throws IOException;
   }

   private static class StringLiteral implements FastDatePrinter.Rule {
      private final String mValue;

      StringLiteral(final String value) {
         this.mValue = value;
      }

      @Override
      public int estimateLength() {
         return this.mValue.length();
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         buffer.append(this.mValue);
      }
   }

   private static class TextField implements FastDatePrinter.Rule {
      private final int mField;
      private final String[] mValues;

      TextField(final int field, final String[] values) {
         this.mField = field;
         this.mValues = values;
      }

      @Override
      public int estimateLength() {
         int max = 0;
         int i = this.mValues.length;

         while (--i >= 0) {
            int len = this.mValues[i].length();
            if (len > max) {
               max = len;
            }
         }

         return max;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         buffer.append(this.mValues[calendar.get(this.mField)]);
      }
   }

   private static class TimeZoneDisplayKey {
      private final TimeZone mTimeZone;
      private final int mStyle;
      private final Locale mLocale;

      TimeZoneDisplayKey(final TimeZone timeZone, final boolean daylight, final int style, final Locale locale) {
         this.mTimeZone = timeZone;
         if (daylight) {
            this.mStyle = style | -2147483648;
         } else {
            this.mStyle = style;
         }

         this.mLocale = locale;
      }

      @Override
      public int hashCode() {
         return (this.mStyle * 31 + this.mLocale.hashCode()) * 31 + this.mTimeZone.hashCode();
      }

      @Override
      public boolean equals(final Object obj) {
         if (this == obj) {
            return true;
         }

         if (!(obj instanceof FastDatePrinter.TimeZoneDisplayKey)) {
            return false;
         }

         FastDatePrinter.TimeZoneDisplayKey other = (FastDatePrinter.TimeZoneDisplayKey)obj;
         return this.mTimeZone.equals(other.mTimeZone) && this.mStyle == other.mStyle && this.mLocale.equals(other.mLocale);
      }
   }

   private static class TimeZoneNameRule implements FastDatePrinter.Rule {
      private final Locale mLocale;
      private final int mStyle;
      private final String mStandard;
      private final String mDaylight;

      TimeZoneNameRule(final TimeZone timeZone, final Locale locale, final int style) {
         this.mLocale = locale;
         this.mStyle = style;
         this.mStandard = FastDatePrinter.getTimeZoneDisplay(timeZone, false, style, locale);
         this.mDaylight = FastDatePrinter.getTimeZoneDisplay(timeZone, true, style, locale);
      }

      @Override
      public int estimateLength() {
         return Math.max(this.mStandard.length(), this.mDaylight.length());
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         TimeZone zone = calendar.getTimeZone();
         if (calendar.get(16) != 0) {
            buffer.append(FastDatePrinter.getTimeZoneDisplay(zone, true, this.mStyle, this.mLocale));
         } else {
            buffer.append(FastDatePrinter.getTimeZoneDisplay(zone, false, this.mStyle, this.mLocale));
         }
      }
   }

   private static class TimeZoneNumberRule implements FastDatePrinter.Rule {
      static final FastDatePrinter.TimeZoneNumberRule INSTANCE_COLON = new FastDatePrinter.TimeZoneNumberRule(true);
      static final FastDatePrinter.TimeZoneNumberRule INSTANCE_NO_COLON = new FastDatePrinter.TimeZoneNumberRule(false);
      final boolean mColon;

      TimeZoneNumberRule(final boolean colon) {
         this.mColon = colon;
      }

      @Override
      public int estimateLength() {
         return 5;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         int offset = calendar.get(15) + calendar.get(16);
         if (offset < 0) {
            buffer.append('-');
            offset = -offset;
         } else {
            buffer.append('+');
         }

         int hours = offset / 3600000;
         FastDatePrinter.appendDigits(buffer, hours);
         if (this.mColon) {
            buffer.append(':');
         }

         int minutes = offset / 60000 - 60 * hours;
         FastDatePrinter.appendDigits(buffer, minutes);
      }
   }

   private static class TwelveHourField implements FastDatePrinter.NumberRule {
      private final FastDatePrinter.NumberRule mRule;

      TwelveHourField(final FastDatePrinter.NumberRule rule) {
         this.mRule = rule;
      }

      @Override
      public int estimateLength() {
         return this.mRule.estimateLength();
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         int value = calendar.get(10);
         if (value == 0) {
            value = calendar.getLeastMaximum(10) + 1;
         }

         this.mRule.appendTo(buffer, value);
      }

      @Override
      public void appendTo(final Appendable buffer, final int value) throws IOException {
         this.mRule.appendTo(buffer, value);
      }
   }

   private static class TwentyFourHourField implements FastDatePrinter.NumberRule {
      private final FastDatePrinter.NumberRule mRule;

      TwentyFourHourField(final FastDatePrinter.NumberRule rule) {
         this.mRule = rule;
      }

      @Override
      public int estimateLength() {
         return this.mRule.estimateLength();
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         int value = calendar.get(11);
         if (value == 0) {
            value = calendar.getMaximum(11) + 1;
         }

         this.mRule.appendTo(buffer, value);
      }

      @Override
      public void appendTo(final Appendable buffer, final int value) throws IOException {
         this.mRule.appendTo(buffer, value);
      }
   }

   private static class TwoDigitMonthField implements FastDatePrinter.NumberRule {
      static final FastDatePrinter.TwoDigitMonthField INSTANCE = new FastDatePrinter.TwoDigitMonthField();

      TwoDigitMonthField() {
      }

      @Override
      public int estimateLength() {
         return 2;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         this.appendTo(buffer, calendar.get(2) + 1);
      }

      @Override
      public final void appendTo(final Appendable buffer, final int value) throws IOException {
         FastDatePrinter.appendDigits(buffer, value);
      }
   }

   private static class TwoDigitNumberField implements FastDatePrinter.NumberRule {
      private final int mField;

      TwoDigitNumberField(final int field) {
         this.mField = field;
      }

      @Override
      public int estimateLength() {
         return 2;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         this.appendTo(buffer, calendar.get(this.mField));
      }

      @Override
      public final void appendTo(final Appendable buffer, final int value) throws IOException {
         if (value < 100) {
            FastDatePrinter.appendDigits(buffer, value);
         } else {
            FastDatePrinter.appendFullDigits(buffer, value, 2);
         }
      }
   }

   private static class TwoDigitYearField implements FastDatePrinter.NumberRule {
      static final FastDatePrinter.TwoDigitYearField INSTANCE = new FastDatePrinter.TwoDigitYearField();

      TwoDigitYearField() {
      }

      @Override
      public int estimateLength() {
         return 2;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         this.appendTo(buffer, calendar.get(1) % 100);
      }

      @Override
      public final void appendTo(final Appendable buffer, final int value) throws IOException {
         FastDatePrinter.appendDigits(buffer, value);
      }
   }

   private static class UnpaddedMonthField implements FastDatePrinter.NumberRule {
      static final FastDatePrinter.UnpaddedMonthField INSTANCE = new FastDatePrinter.UnpaddedMonthField();

      UnpaddedMonthField() {
      }

      @Override
      public int estimateLength() {
         return 2;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         this.appendTo(buffer, calendar.get(2) + 1);
      }

      @Override
      public final void appendTo(final Appendable buffer, final int value) throws IOException {
         if (value < 10) {
            buffer.append((char)(value + 48));
         } else {
            FastDatePrinter.appendDigits(buffer, value);
         }
      }
   }

   private static class UnpaddedNumberField implements FastDatePrinter.NumberRule {
      private final int mField;

      UnpaddedNumberField(final int field) {
         this.mField = field;
      }

      @Override
      public int estimateLength() {
         return 4;
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         this.appendTo(buffer, calendar.get(this.mField));
      }

      @Override
      public final void appendTo(final Appendable buffer, final int value) throws IOException {
         if (value < 10) {
            buffer.append((char)(value + 48));
         } else if (value < 100) {
            FastDatePrinter.appendDigits(buffer, value);
         } else {
            FastDatePrinter.appendFullDigits(buffer, value, 1);
         }
      }
   }

   private static class WeekYear implements FastDatePrinter.NumberRule {
      private final FastDatePrinter.NumberRule mRule;

      WeekYear(final FastDatePrinter.NumberRule rule) {
         this.mRule = rule;
      }

      @Override
      public int estimateLength() {
         return this.mRule.estimateLength();
      }

      @Override
      public void appendTo(final Appendable buffer, final Calendar calendar) throws IOException {
         this.mRule.appendTo(buffer, calendar.getWeekYear());
      }

      @Override
      public void appendTo(final Appendable buffer, final int value) throws IOException {
         this.mRule.appendTo(buffer, value);
      }
   }
}
