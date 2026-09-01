package org.apache.logging.log4j.core.time.internal.format;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;

public abstract class Format {
   public final String format(final Object obj) {
      return this.format(obj, new StringBuilder(), new FieldPosition(0)).toString();
   }

   public abstract StringBuilder format(Object obj, StringBuilder toAppendTo, FieldPosition pos);

   public abstract Object parseObject(String source, ParsePosition pos);

   public Object parseObject(final String source) throws ParseException {
      ParsePosition pos = new ParsePosition(0);
      Object result = this.parseObject(source, pos);
      if (pos.getIndex() == 0) {
         throw new ParseException("Format.parseObject(String) failed", pos.getErrorIndex());
      } else {
         return result;
      }
   }
}
