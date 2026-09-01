package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.Duration;
import java.time.format.DateTimeParseException;

public class DurationConverter extends AbstractSingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return Duration.class == type;
   }

   public Duration fromString(String str) {
      try {
         return Duration.parse(str);
      } catch (DateTimeParseException ex) {
         ConversionException exception = new ConversionException("Cannot parse value as duration", ex);
         exception.add("value", str);
         throw exception;
      }
   }
}
