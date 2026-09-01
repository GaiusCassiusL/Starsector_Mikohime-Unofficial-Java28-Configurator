package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("Converter")
@Plugin("LineLocationPatternConverter")
@ConverterKeys({"L", "line"})
public final class LineLocationPatternConverter extends LogEventPatternConverter {
   private static final LineLocationPatternConverter INSTANCE = new LineLocationPatternConverter();

   private LineLocationPatternConverter() {
      super("Line", "line");
   }

   public static LineLocationPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   @Override
   public void format(final LogEvent event, final StringBuilder output) {
      StackTraceElement element = event.getSource();
      if (element != null) {
         output.append(element.getLineNumber());
      }
   }

   @Override
   public boolean requiresLocation() {
      return true;
   }
}
