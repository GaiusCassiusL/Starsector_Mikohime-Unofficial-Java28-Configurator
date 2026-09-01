package org.apache.logging.log4j.core.pattern;

import java.util.Date;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("FileConverter")
@Plugin("IntegerPatternConverter")
@ConverterKeys({"i", "index"})
@PerformanceSensitive("allocation")
public final class IntegerPatternConverter extends AbstractPatternConverter implements ArrayPatternConverter {
   private static final IntegerPatternConverter INSTANCE = new IntegerPatternConverter();

   private IntegerPatternConverter() {
      super("Integer", "integer");
   }

   public static IntegerPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   @Override
   public void format(final StringBuilder toAppendTo, final Object... objects) {
      for (int i = 0; i < objects.length; i++) {
         if (objects[i] instanceof Integer) {
            this.format(objects[i], toAppendTo);
            break;
         }

         if (objects[i] instanceof NotANumber) {
            toAppendTo.append("\u0000");
            break;
         }
      }
   }

   @Override
   public void format(final Object obj, final StringBuilder toAppendTo) {
      if (obj instanceof Integer) {
         toAppendTo.append(((Integer)obj).intValue());
      } else if (obj instanceof Date) {
         toAppendTo.append(((Date)obj).getTime());
      }
   }
}
