package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.Year;

public class YearConverter extends AbstractSingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return Year.class == type;
   }

   public Year fromString(String str) {
      try {
         return Year.of(Integer.parseInt(str));
      } catch (NumberFormatException ex) {
         ConversionException exception = new ConversionException("Cannot parse value as year", ex);
         exception.add("value", str);
         throw exception;
      }
   }
}
