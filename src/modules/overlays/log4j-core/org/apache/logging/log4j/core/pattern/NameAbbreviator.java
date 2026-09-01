package org.apache.logging.log4j.core.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.util.PerformanceSensitive;

@PerformanceSensitive("allocation")
public abstract class NameAbbreviator {
   private static final NameAbbreviator DEFAULT = new NameAbbreviator.NOPAbbreviator();

   public static NameAbbreviator getAbbreviator(final String pattern) {
      if (pattern.length() <= 0) {
         return DEFAULT;
      }

      String trimmed = pattern.trim();
      if (trimmed.isEmpty()) {
         return DEFAULT;
      }

      NameAbbreviator dwa = DynamicWordAbbreviator.create(trimmed);
      if (dwa != null) {
         return dwa;
      }

      boolean isNegativeNumber;
      String number;
      if (trimmed.length() > 1 && trimmed.charAt(0) == '-') {
         isNegativeNumber = true;
         number = trimmed.substring(1);
      } else {
         isNegativeNumber = false;
         number = trimmed;
      }

      int i = 0;

      while (i < number.length() && number.charAt(i) >= '0' && number.charAt(i) <= '9') {
         i++;
      }

      if (i == number.length()) {
         return new NameAbbreviator.MaxElementAbbreviator(
            Integer.parseInt(number),
            isNegativeNumber ? NameAbbreviator.MaxElementAbbreviator.Strategy.DROP : NameAbbreviator.MaxElementAbbreviator.Strategy.RETAIN
         );
      }

      ArrayList<NameAbbreviator.PatternAbbreviatorFragment> fragments = new ArrayList<>(5);

      for (int pos = 0; pos < trimmed.length() && pos >= 0; pos++) {
         int ellipsisPos = pos;
         int charCount;
         if (trimmed.charAt(pos) == '*') {
            charCount = Integer.MAX_VALUE;
            ellipsisPos++;
         } else if (trimmed.charAt(pos) >= '0' && trimmed.charAt(pos) <= '9') {
            charCount = trimmed.charAt(pos) - '0';
            ellipsisPos++;
         } else {
            charCount = 0;
         }

         char ellipsis = 0;
         if (ellipsisPos < trimmed.length()) {
            ellipsis = trimmed.charAt(ellipsisPos);
            if (ellipsis == '.') {
               ellipsis = 0;
            }
         }

         fragments.add(new NameAbbreviator.PatternAbbreviatorFragment(charCount, ellipsis));
         pos = trimmed.indexOf(46, pos);
         if (pos == -1) {
            break;
         }
      }

      return new NameAbbreviator.PatternAbbreviator(fragments);
   }

   public static NameAbbreviator getDefaultAbbreviator() {
      return DEFAULT;
   }

   public abstract void abbreviate(final String original, final StringBuilder destination);

   private static class MaxElementAbbreviator extends NameAbbreviator {
      private final int count;
      private final NameAbbreviator.MaxElementAbbreviator.Strategy strategy;

      public MaxElementAbbreviator(final int count, final NameAbbreviator.MaxElementAbbreviator.Strategy strategy) {
         this.count = Math.max(count, strategy.minCount);
         this.strategy = strategy;
      }

      @Override
      public void abbreviate(final String original, final StringBuilder destination) {
         this.strategy.abbreviate(this.count, original, destination);
      }

      private enum Strategy {
         DROP(0) {
            @Override
            void abbreviate(final int count, final String original, final StringBuilder destination) {
               int start = 0;

               for (int i = 0; i < count; i++) {
                  int nextStart = original.indexOf(46, start);
                  if (nextStart == -1) {
                     destination.append(original);
                     return;
                  }

                  start = nextStart + 1;
               }

               destination.append(original, start, original.length());
            }
         },
         RETAIN(1) {
            @Override
            void abbreviate(final int count, final String original, final StringBuilder destination) {
               int end = original.length() - 1;

               for (int i = count; i > 0; i--) {
                  end = original.lastIndexOf(46, end - 1);
                  if (end == -1) {
                     destination.append(original);
                     return;
                  }
               }

               destination.append(original, end + 1, original.length());
            }
         };

         final int minCount;

         Strategy(final int minCount) {
            this.minCount = minCount;
         }

         abstract void abbreviate(final int count, final String original, final StringBuilder destination);
      }
   }

   private static class NOPAbbreviator extends NameAbbreviator {
      public NOPAbbreviator() {
      }

      @Override
      public void abbreviate(final String original, final StringBuilder destination) {
         destination.append(original);
      }
   }

   private static final class PatternAbbreviator extends NameAbbreviator {
      private final NameAbbreviator.PatternAbbreviatorFragment[] fragments;

      PatternAbbreviator(final List<NameAbbreviator.PatternAbbreviatorFragment> fragments) {
         if (fragments.isEmpty()) {
            throw new IllegalArgumentException("fragments must have at least one element");
         }

         this.fragments = fragments.toArray(NameAbbreviator.PatternAbbreviatorFragment.EMPTY_ARRAY);
      }

      @Override
      public void abbreviate(final String original, final StringBuilder destination) {
         int originalIndex = 0;
         int iteration = 0;
         int originalLength = original.length();

         while (originalIndex >= 0 && originalIndex < originalLength) {
            originalIndex = this.fragment(iteration++).abbreviate(original, originalIndex, destination);
         }
      }

      NameAbbreviator.PatternAbbreviatorFragment fragment(final int index) {
         return this.fragments[Math.min(index, this.fragments.length - 1)];
      }

      @Override
      public String toString() {
         return String.format("%s[fragments=%s]", this.getClass().getSimpleName(), Arrays.toString(this.fragments));
      }
   }

   private static final class PatternAbbreviatorFragment {
      static final NameAbbreviator.PatternAbbreviatorFragment[] EMPTY_ARRAY = new NameAbbreviator.PatternAbbreviatorFragment[0];
      private final int charCount;
      private final char ellipsis;

      PatternAbbreviatorFragment(final int charCount, final char ellipsis) {
         this.charCount = charCount;
         this.ellipsis = ellipsis;
      }

      int abbreviate(final String input, final int inputIndex, final StringBuilder buf) {
         int nextDot = input.indexOf(46, inputIndex);
         if (nextDot < 0) {
            buf.append(input, inputIndex, input.length());
            return nextDot;
         }

         if (nextDot - inputIndex > this.charCount) {
            buf.append(input, inputIndex, inputIndex + this.charCount);
            if (this.ellipsis != 0) {
               buf.append(this.ellipsis);
            }

            buf.append('.');
         } else {
            buf.append(input, inputIndex, nextDot + 1);
         }

         return nextDot + 1;
      }

      @Override
      public String toString() {
         return String.format("%s[charCount=%s, ellipsis=%s]", this.getClass().getSimpleName(), this.charCount, Integer.toHexString(this.ellipsis));
      }
   }
}
