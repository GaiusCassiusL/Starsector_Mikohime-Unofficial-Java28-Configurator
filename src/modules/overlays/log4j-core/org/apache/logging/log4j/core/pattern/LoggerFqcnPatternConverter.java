package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("LoggerFqcnPatternConverter")
@ConverterKeys("fqcn")
@PerformanceSensitive("allocation")
public final class LoggerFqcnPatternConverter extends LogEventPatternConverter {
   private static final LoggerFqcnPatternConverter INSTANCE = new LoggerFqcnPatternConverter();

   private LoggerFqcnPatternConverter() {
      super("LoggerFqcn", "loggerFqcn");
   }

   public static LoggerFqcnPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      toAppendTo.append(event.getLoggerFqcn());
   }
}
