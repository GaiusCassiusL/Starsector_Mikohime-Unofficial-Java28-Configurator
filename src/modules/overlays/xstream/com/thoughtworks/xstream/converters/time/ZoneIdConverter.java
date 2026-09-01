package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

public class ZoneIdConverter implements SingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return type != null && ZoneId.class.isAssignableFrom(type);
   }

   public ZoneId fromString(String str) {
      ConversionException exception;
      try {
         return ZoneId.of(str);
      } catch (ZoneRulesException e) {
         exception = new ConversionException("Not a valid zone id", e);
      } catch (DateTimeException e) {
         exception = new ConversionException("Cannot parse value as zone id", e);
      }

      exception.add("value", str);
      throw exception;
   }

   @Override
   public String toString(Object obj) {
      if (obj == null) {
         return null;
      }

      ZoneId zoneId = (ZoneId)obj;
      return zoneId.getId();
   }
}
