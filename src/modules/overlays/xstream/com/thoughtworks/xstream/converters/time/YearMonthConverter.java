package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class YearMonthConverter extends AbstractSingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return YearMonth.class == type;
   }

   public YearMonth fromString(String str) {
      try {
         return YearMonth.parse(str);
      } catch (DateTimeParseException ex) {
         ConversionException exception = new ConversionException("Cannot parse value as year month", ex);
         exception.add("value", str);
         throw exception;
      }
   }
}
