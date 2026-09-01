package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.core.util.ThreadSafeSimpleDateFormat;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.TimeZone;

public class SqlTimestampConverter extends AbstractSingleValueConverter {
   private final ThreadSafeSimpleDateFormat format;

   public SqlTimestampConverter() {
      this(TimeZone.getTimeZone("UTC"));
   }

   public SqlTimestampConverter(TimeZone timeZone) {
      this.format = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss", timeZone, 0, 5, false);
   }

   public boolean canConvert(Class type) {
      return type == Timestamp.class;
   }

   public String toString(Object obj) {
      Timestamp timestamp = (Timestamp)obj;
      StringBuffer buffer = new StringBuffer(this.format.format(timestamp));
      if (timestamp.getNanos() != 0) {
         buffer.append('.');
         String nanos = String.valueOf(timestamp.getNanos() + 1000000000);
         int last = 10;

         while (last > 2 && nanos.charAt(last - 1) == '0') {
            last--;
         }

         buffer.append(nanos.subSequence(1, last));
      }

      return buffer.toString();
   }

   public Object fromString(String str) {
      int idx = str.lastIndexOf(46);
      if (idx <= 0 || str.length() - idx >= 2 && str.length() - idx <= 10) {
         try {
            Timestamp timestamp = new Timestamp(this.format.parse(idx < 0 ? str : str.substring(0, idx)).getTime());
            if (idx > 0) {
               StringBuffer buffer = new StringBuffer(str.substring(idx + 1));

               while (buffer.length() != 9) {
                  buffer.append('0');
               }

               timestamp.setNanos(Integer.parseInt(buffer.toString()));
            }

            return timestamp;
         } catch (NumberFormatException e) {
            throw new ConversionException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]", e);
         } catch (ParseException e) {
            throw new ConversionException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]", e);
         }
      } else {
         throw new ConversionException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]");
      }
   }
}
