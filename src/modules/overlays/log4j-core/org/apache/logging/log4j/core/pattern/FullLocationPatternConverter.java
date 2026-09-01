package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("Converter")
@Plugin("FullLocationPatternConverter")
@ConverterKeys({"l", "location"})
public final class FullLocationPatternConverter extends LogEventPatternConverter {
   private static final FullLocationPatternConverter INSTANCE = new FullLocationPatternConverter();

   private FullLocationPatternConverter() {
      super("Full Location", "fullLocation");
   }

   public static FullLocationPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   @Override
   public void format(final LogEvent event, final StringBuilder output) {
      StackTraceElement element = event.getSource();
      if (element != null) {
         output.append(element.toString());
      }
   }

   @Override
   public boolean requiresLocation() {
      return true;
   }
}
