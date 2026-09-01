package org.apache.logging.log4j.core.pattern;

import java.util.Objects;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.MapMessage.MapFormat;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("Converter")
@Plugin("MapPatternConverter")
@ConverterKeys({"K", "map", "MAP"})
public final class MapPatternConverter extends LogEventPatternConverter {
   private static final String JAVA_UNQUOTED = MapFormat.JAVA_UNQUOTED.name();
   private final String key;
   private final String[] format;

   private MapPatternConverter(final String[] options, final String... format) {
      super(options != null && options.length > 0 ? "MAP{" + options[0] + "}" : "MAP", "map");
      this.key = options != null && options.length > 0 ? options[0] : null;
      this.format = format;
   }

   public static MapPatternConverter newInstance(final String[] options) {
      return new MapPatternConverter(options, JAVA_UNQUOTED);
   }

   public static MapPatternConverter newInstance(final String[] options, final MapFormat format) {
      return new MapPatternConverter(options, Objects.toString(format, JAVA_UNQUOTED));
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      if (event.getMessage() instanceof MapMessage) {
         MapMessage msg = (MapMessage)event.getMessage();
         if (this.key == null) {
            msg.formatTo(this.format, toAppendTo);
         } else {
            String val = msg.get(this.key);
            if (val != null) {
               toAppendTo.append(val);
            }
         }
      }
   }
}
