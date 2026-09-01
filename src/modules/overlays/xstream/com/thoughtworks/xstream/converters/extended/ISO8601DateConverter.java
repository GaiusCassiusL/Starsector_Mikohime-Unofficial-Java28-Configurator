package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class ISO8601DateConverter extends AbstractSingleValueConverter {
   private final ISO8601GregorianCalendarConverter converter = new ISO8601GregorianCalendarConverter();

   public boolean canConvert(Class type) {
      return type == Date.class && this.converter.canConvert(GregorianCalendar.class);
   }

   public Object fromString(String str) {
      return ((Calendar)this.converter.fromString(str)).getTime();
   }

   public String toString(Object obj) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime((Date)obj);
      return this.converter.toString(calendar);
   }
}
