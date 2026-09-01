package com.thoughtworks.xstream.converters.extended;

import java.sql.Timestamp;
import java.util.Date;

public class ISO8601SqlTimestampConverter extends ISO8601DateConverter {
   private static final String PADDING = "000000000";

   public boolean canConvert(Class type) {
      return type == Timestamp.class && super.canConvert(Date.class);
   }

   public Object fromString(String str) {
      int idxFraction = str.lastIndexOf(46);
      int nanos = 0;
      if (idxFraction > 0) {
         int idx = idxFraction + 1;

         while (Character.isDigit(str.charAt(idx))) {
            idx++;
         }

         nanos = Integer.parseInt(str.substring(idxFraction + 1, idx));
         str = str.substring(0, idxFraction) + str.substring(idx);
      }

      Date date = (Date)super.fromString(str);
      Timestamp timestamp = new Timestamp(date.getTime());
      timestamp.setNanos(nanos);
      return timestamp;
   }

   public String toString(Object obj) {
      Timestamp timestamp = (Timestamp)obj;
      String str = super.toString(new Date(timestamp.getTime() / 1000L * 1000L));
      String nanos = String.valueOf(timestamp.getNanos());
      int idxFraction = str.lastIndexOf(46);
      return str.substring(0, idxFraction + 1) + "000000000".substring(nanos.length()) + nanos + str.substring(idxFraction + 4);
   }
}
