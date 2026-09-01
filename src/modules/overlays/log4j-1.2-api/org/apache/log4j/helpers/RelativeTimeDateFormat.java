package org.apache.log4j.helpers;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;

public class RelativeTimeDateFormat extends DateFormat {
   private static final long serialVersionUID = 7055751607085611984L;
   protected final long startTime = System.currentTimeMillis();

   @Override
   public StringBuffer format(final Date date, final StringBuffer sbuf, final FieldPosition fieldPosition) {
      return sbuf.append(date.getTime() - this.startTime);
   }

   @Override
   public Date parse(final String s, final ParsePosition pos) {
      return null;
   }
}
