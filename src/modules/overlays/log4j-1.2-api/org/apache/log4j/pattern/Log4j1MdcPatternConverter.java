package org.apache.log4j.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.TriConsumer;

@Namespace("Converter")
@Plugin("Log4j1MdcPatternConverter")
@ConverterKeys("properties")
public final class Log4j1MdcPatternConverter extends LogEventPatternConverter {
   private final String key;
   private static TriConsumer<String, Object, StringBuilder> APPEND_EACH = (key, value, toAppendTo) -> toAppendTo.append('{')
      .append(key)
      .append(',')
      .append(value)
      .append('}');

   private Log4j1MdcPatternConverter(final String[] options) {
      super(options != null && options.length > 0 ? "Log4j1MDC{" + options[0] + "}" : "Log4j1MDC", "property");
      if (options != null && options.length > 0) {
         this.key = options[0];
      } else {
         this.key = null;
      }
   }

   public static Log4j1MdcPatternConverter newInstance(final String[] options) {
      return new Log4j1MdcPatternConverter(options);
   }

   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      if (this.key == null) {
         toAppendTo.append('{');
         event.getContextData().forEach(APPEND_EACH, toAppendTo);
         toAppendTo.append('}');
      } else {
         Object val = event.getContextData().getValue(this.key);
         if (val != null) {
            toAppendTo.append(val);
         }
      }
   }
}
