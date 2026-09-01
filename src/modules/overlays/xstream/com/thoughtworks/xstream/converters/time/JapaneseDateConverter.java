package com.thoughtworks.xstream.converters.time;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.util.Collections;

public class JapaneseDateConverter extends AbstractChronoLocalDateConverter<JapaneseEra> {
   @Override
   public boolean canConvert(Class type) {
      return JapaneseDate.class == type;
   }

   @Override
   public Object fromString(String str) {
      return this.parseChronoLocalDate(str, "Japanese", Collections.singleton(JapaneseChronology.INSTANCE));
   }

   protected ChronoLocalDate chronoLocalDateOf(JapaneseEra era, int prolepticYear, int month, int dayOfMonth) {
      return JapaneseDate.of(era, prolepticYear, month, dayOfMonth);
   }

   protected JapaneseEra eraOf(String id) {
      return JapaneseEra.valueOf(id);
   }
}
