package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.OptionConverter;
import org.apache.logging.log4j.util.PerformanceSensitive;

@PerformanceSensitive("allocation")
abstract class SimpleLiteralPatternConverter extends LogEventPatternConverter implements ArrayPatternConverter {
   private SimpleLiteralPatternConverter() {
      super("SimpleLiteral", "literal");
   }

   static LogEventPatternConverter of(final String literal, final boolean convertBackslashes) {
      String value = convertBackslashes ? OptionConverter.convertSpecialChars(literal) : literal;
      return of(value);
   }

   static LogEventPatternConverter of(final String literal) {
      if (literal == null || literal.isEmpty()) {
         return SimpleLiteralPatternConverter.Noop.INSTANCE;
      } else {
         return " ".equals(literal) ? SimpleLiteralPatternConverter.Space.INSTANCE : new SimpleLiteralPatternConverter.StringValue(literal);
      }
   }

   @Override
   public final void format(final LogEvent ignored, final StringBuilder output) {
      this.format(output);
   }

   @Override
   public final void format(final Object ignored, final StringBuilder output) {
      this.format(output);
   }

   @Override
   public final void format(final StringBuilder output, final Object... args) {
      this.format(output);
   }

   abstract void format(final StringBuilder output);

   @Override
   public final boolean isVariable() {
      return false;
   }

   @Override
   public final boolean handlesThrowable() {
      return false;
   }

   private static final class Noop extends SimpleLiteralPatternConverter {
      private static final SimpleLiteralPatternConverter.Noop INSTANCE = new SimpleLiteralPatternConverter.Noop();

      @Override
      void format(final StringBuilder output) {
      }
   }

   private static final class Space extends SimpleLiteralPatternConverter {
      private static final SimpleLiteralPatternConverter.Space INSTANCE = new SimpleLiteralPatternConverter.Space();

      @Override
      void format(final StringBuilder output) {
         output.append(' ');
      }
   }

   private static final class StringValue extends SimpleLiteralPatternConverter {
      private final String literal;

      StringValue(final String literal) {
         this.literal = literal;
      }

      @Override
      void format(final StringBuilder output) {
         output.append(this.literal);
      }
   }
}
