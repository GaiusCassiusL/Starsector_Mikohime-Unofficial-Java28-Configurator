package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.DateTimeException;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractChronoLocalDateConverter<E extends Era> extends AbstractSingleValueConverter {
   private static final Pattern CHRONO_DATE_PATTERN = Pattern.compile("^ (\\w+) (\\d+)-(\\d+)-(\\d+)$");

   protected abstract ChronoLocalDate chronoLocalDateOf(E var1, int var2, int var3, int var4);

   protected abstract E eraOf(String var1);

   protected ChronoLocalDate parseChronoLocalDate(String str, String dateTypeName, Set<Chronology> chronologies) {
      if (str == null) {
         return null;
      }

      ConversionException exception = null;

      for (Chronology chronology : chronologies) {
         String id = chronology.getId();
         if (str.startsWith(id + ' ')) {
            Matcher matcher = CHRONO_DATE_PATTERN.matcher(str.subSequence(id.length(), str.length()));
            if (matcher.matches()) {
               E era = null;

               try {
                  era = this.eraOf(matcher.group(1));
               } catch (IllegalArgumentException e) {
                  exception = new ConversionException("Cannot parse value as " + dateTypeName + " date", e);
                  break;
               }

               if (era != null) {
                  try {
                     return this.chronoLocalDateOf(
                        era, Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4))
                     );
                  } catch (DateTimeException e) {
                     exception = new ConversionException("Cannot parse value as " + dateTypeName + " date", e);
                     break;
                  }
               }
            }
         }
      }

      if (exception == null) {
         exception = new ConversionException("Cannot parse value as " + dateTypeName + " date");
      }

      exception.add("value", str);
      throw exception;
   }
}
