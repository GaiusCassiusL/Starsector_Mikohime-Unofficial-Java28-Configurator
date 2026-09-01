package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

public class OffsetTimeConverter implements SingleValueConverter {
   private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("HH:mm:ss")
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
      .appendOffsetId()
      .toFormatter();

   @Override
   public boolean canConvert(Class type) {
      return OffsetTime.class == type;
   }

   @Override
   public Object fromString(String str) {
      try {
         return OffsetTime.parse(str);
      } catch (DateTimeParseException e) {
         ConversionException exception = new ConversionException("Cannot parse value as offset time", e);
         exception.add("value", str);
         throw exception;
      }
   }

   @Override
   public String toString(Object obj) {
      if (obj == null) {
         return null;
      }

      OffsetTime offsetTime = (OffsetTime)obj;
      return FORMATTER.format(offsetTime);
   }
}
