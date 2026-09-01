package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.Period;
import java.time.format.DateTimeParseException;

public class PeriodConverter extends AbstractSingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return Period.class == type;
   }

   public Period fromString(String str) {
      try {
         return Period.parse(str);
      } catch (DateTimeParseException ex) {
         ConversionException exception = new ConversionException("Cannot parse period value", ex);
         exception.add("period", str);
         throw exception;
      }
   }
}
