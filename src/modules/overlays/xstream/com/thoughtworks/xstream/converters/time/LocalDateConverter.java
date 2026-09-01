package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class LocalDateConverter extends AbstractSingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return LocalDate.class == type;
   }

   @Override
   public Object fromString(String str) {
      try {
         return LocalDate.parse(str);
      } catch (DateTimeParseException e) {
         ConversionException exception = new ConversionException("Cannot parse value as local date", e);
         exception.add("value", str);
         throw exception;
      }
   }
}
