package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("LoggerPatternConverter")
@ConverterKeys({"c", "logger"})
@PerformanceSensitive("allocation")
public final class LoggerPatternConverter extends NamePatternConverter {
   private static final LoggerPatternConverter INSTANCE = new LoggerPatternConverter(null);

   private LoggerPatternConverter(final String[] options) {
      super("Logger", "logger", options);
   }

   public static LoggerPatternConverter newInstance(final String[] options) {
      return options != null && options.length != 0 ? new LoggerPatternConverter(options) : INSTANCE;
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      this.abbreviate(event.getLoggerName(), toAppendTo);
   }
}
