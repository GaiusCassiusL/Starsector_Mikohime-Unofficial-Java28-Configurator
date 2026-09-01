package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import java.time.DateTimeException;
import java.time.chrono.Chronology;

public class ChronologyConverter implements SingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return type != null && Chronology.class.isAssignableFrom(type);
   }

   public Chronology fromString(String str) {
      if (str == null) {
         return null;
      }

      try {
         return Chronology.of(str);
      } catch (DateTimeException e) {
         ConversionException exception = new ConversionException("Cannot parse value as chronology", e);
         exception.add("value", str);
         throw exception;
      }
   }

   @Override
   public String toString(Object obj) {
      return obj == null ? null : ((Chronology)obj).getId();
   }
}
