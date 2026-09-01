package org.apache.logging.log4j.core.lookup;

import java.util.Arrays;
import org.apache.logging.log4j.util.Strings;

public abstract class StrMatcher {
   private static final StrMatcher COMMA_MATCHER = new StrMatcher.CharMatcher(',');
   private static final StrMatcher TAB_MATCHER = new StrMatcher.CharMatcher('\t');
   private static final StrMatcher SPACE_MATCHER = new StrMatcher.CharMatcher(' ');
   private static final StrMatcher SPLIT_MATCHER = new StrMatcher.CharSetMatcher(" \t\n\r\f".toCharArray());
   private static final StrMatcher TRIM_MATCHER = new StrMatcher.TrimMatcher();
   private static final StrMatcher SINGLE_QUOTE_MATCHER = new StrMatcher.CharMatcher('\'');
   private static final StrMatcher DOUBLE_QUOTE_MATCHER = new StrMatcher.CharMatcher('"');
   private static final StrMatcher QUOTE_MATCHER = new StrMatcher.CharSetMatcher("'\"".toCharArray());
   private static final StrMatcher NONE_MATCHER = new StrMatcher.NoMatcher();

   protected StrMatcher() {
   }

   public static StrMatcher commaMatcher() {
      return COMMA_MATCHER;
   }

   public static StrMatcher tabMatcher() {
      return TAB_MATCHER;
   }

   public static StrMatcher spaceMatcher() {
      return SPACE_MATCHER;
   }

   public static StrMatcher splitMatcher() {
      return SPLIT_MATCHER;
   }

   public static StrMatcher trimMatcher() {
      return TRIM_MATCHER;
   }

   public static StrMatcher singleQuoteMatcher() {
      return SINGLE_QUOTE_MATCHER;
   }

   public static StrMatcher doubleQuoteMatcher() {
      return DOUBLE_QUOTE_MATCHER;
   }

   public static StrMatcher quoteMatcher() {
      return QUOTE_MATCHER;
   }

   public static StrMatcher noneMatcher() {
      return NONE_MATCHER;
   }

   public static StrMatcher charMatcher(final char ch) {
      return new StrMatcher.CharMatcher(ch);
   }

   public static StrMatcher charSetMatcher(final char[] chars) {
      if (chars != null && chars.length != 0) {
         return chars.length == 1 ? new StrMatcher.CharMatcher(chars[0]) : new StrMatcher.CharSetMatcher(chars);
      } else {
         return NONE_MATCHER;
      }
   }

   public static StrMatcher charSetMatcher(final String chars) {
      if (Strings.isEmpty(chars)) {
         return NONE_MATCHER;
      } else {
         return chars.length() == 1 ? new StrMatcher.CharMatcher(chars.charAt(0)) : new StrMatcher.CharSetMatcher(chars.toCharArray());
      }
   }

   public static StrMatcher stringMatcher(final String str) {
      return Strings.isEmpty(str) ? NONE_MATCHER : new StrMatcher.StringMatcher(str);
   }

   public abstract int isMatch(char[] buffer, int pos, int bufferStart, int bufferEnd);

   public int isMatch(final char[] buffer, final int pos) {
      return this.isMatch(buffer, pos, 0, buffer.length);
   }

   static final class CharMatcher extends StrMatcher {
      private final char ch;

      CharMatcher(final char ch) {
         this.ch = ch;
      }

      @Override
      public int isMatch(final char[] buffer, final int pos, final int bufferStart, final int bufferEnd) {
         return this.ch == buffer[pos] ? 1 : 0;
      }
   }

   static final class CharSetMatcher extends StrMatcher {
      private final char[] chars;

      CharSetMatcher(final char[] chars) {
         this.chars = (char[])chars.clone();
         Arrays.sort(this.chars);
      }

      @Override
      public int isMatch(final char[] buffer, final int pos, final int bufferStart, final int bufferEnd) {
         return Arrays.binarySearch(this.chars, buffer[pos]) >= 0 ? 1 : 0;
      }
   }

   static final class NoMatcher extends StrMatcher {
      @Override
      public int isMatch(final char[] buffer, final int pos, final int bufferStart, final int bufferEnd) {
         return 0;
      }
   }

   static final class StringMatcher extends StrMatcher {
      private final char[] chars;

      StringMatcher(final String str) {
         this.chars = str.toCharArray();
      }

      @Override
      public int isMatch(final char[] buffer, final int pos, final int bufferStart, final int bufferEnd) {
         int len = this.chars.length;
         if (pos + len > bufferEnd) {
            return 0;
         }

         int p = pos;

         for (int i = 0; i < this.chars.length; p++) {
            if (this.chars[i] != buffer[p]) {
               return 0;
            }

            i++;
         }

         return len;
      }

      @Override
      public String toString() {
         return super.toString() + " " + Arrays.toString(this.chars);
      }
   }

   static final class TrimMatcher extends StrMatcher {
      @Override
      public int isMatch(final char[] buffer, final int pos, final int bufferStart, final int bufferEnd) {
         return buffer[pos] <= 32 ? 1 : 0;
      }
   }
}
