package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("Converter")
@Plugin("MethodLocationPatternConverter")
@ConverterKeys({"M", "method"})
public final class MethodLocationPatternConverter extends LogEventPatternConverter {
   private static final MethodLocationPatternConverter INSTANCE = new MethodLocationPatternConverter();

   private MethodLocationPatternConverter() {
      super("Method", "method");
   }

   public static MethodLocationPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      StackTraceElement element = event.getSource();
      if (element != null) {
         toAppendTo.append(element.getMethodName());
      }
   }

   @Override
   public boolean requiresLocation() {
      return true;
   }
}
