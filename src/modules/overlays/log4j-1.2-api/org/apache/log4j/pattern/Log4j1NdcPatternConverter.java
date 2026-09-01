package org.apache.log4j.pattern;

import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.Strings;

@Namespace("Converter")
@Plugin("Log4j1NdcPatternConverter")
@ConverterKeys("ndc")
public final class Log4j1NdcPatternConverter extends LogEventPatternConverter {
   private static final Log4j1NdcPatternConverter INSTANCE = new Log4j1NdcPatternConverter();

   private Log4j1NdcPatternConverter() {
      super("Log4j1NDC", "ndc");
   }

   public static Log4j1NdcPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      List<String> ndc = event.getContextStack().asList();
      toAppendTo.append(Strings.join(ndc, ' '));
   }
}
