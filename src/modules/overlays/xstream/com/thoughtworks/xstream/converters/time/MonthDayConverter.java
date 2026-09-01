package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;

public class MonthDayConverter extends AbstractSingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return MonthDay.class == type;
   }

   public MonthDay fromString(String str) {
      try {
         return MonthDay.parse(str);
      } catch (DateTimeParseException ex) {
         ConversionException exception = new ConversionException("Cannot parse value as month day", ex);
         exception.add("value", str);
         throw exception;
      }
   }
}
