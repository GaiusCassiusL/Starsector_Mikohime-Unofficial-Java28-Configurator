package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("EndOfBatchPatternConverter")
@ConverterKeys("endOfBatch")
@PerformanceSensitive("allocation")
public final class EndOfBatchPatternConverter extends LogEventPatternConverter {
   private static final EndOfBatchPatternConverter INSTANCE = new EndOfBatchPatternConverter();

   private EndOfBatchPatternConverter() {
      super("LoggerFqcn", "loggerFqcn");
   }

   public static EndOfBatchPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      toAppendTo.append(event.isEndOfBatch());
   }
}
