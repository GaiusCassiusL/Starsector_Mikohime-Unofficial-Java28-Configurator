package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class InstantConverter extends AbstractSingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return Instant.class == type;
   }

   public Instant fromString(String str) {
      try {
         return Instant.parse(str);
      } catch (DateTimeParseException ex) {
         ConversionException exception = new ConversionException("Cannot parse value as instant", ex);
         exception.add("value", str);
         throw exception;
      }
   }
}
