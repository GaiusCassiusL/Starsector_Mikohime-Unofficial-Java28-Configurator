package org.apache.logging.log4j.core.time.internal.format;

import java.text.FieldPosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public interface DatePrinter {
   String format(long millis);

   String format(Date date);

   String format(Calendar calendar);

   <B extends Appendable> B format(long millis, B buf);

   <B extends Appendable> B format(Date date, B buf);

   <B extends Appendable> B format(Calendar calendar, B buf);

   String getPattern();

   TimeZone getTimeZone();

   Locale getLocale();

   StringBuilder format(Object obj, StringBuilder toAppendTo, FieldPosition pos);
}
