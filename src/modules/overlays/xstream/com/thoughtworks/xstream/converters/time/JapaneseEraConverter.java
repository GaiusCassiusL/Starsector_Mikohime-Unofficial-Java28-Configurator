package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.chrono.JapaneseEra;

public class JapaneseEraConverter extends AbstractSingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return type != null && JapaneseEra.class.isAssignableFrom(type);
   }

   public JapaneseEra fromString(String str) {
      if (str == null) {
         return null;
      }

      try {
         return JapaneseEra.valueOf(str);
      } catch (IllegalArgumentException e) {
         ConversionException exception = new ConversionException("Cannot parse value as Japanese era", e);
         exception.add("value", str);
         throw exception;
      }
   }
}
