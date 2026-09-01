package com.thoughtworks.xstream.converters.time;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoDate;
import java.time.chrono.MinguoEra;
import java.util.Collections;

public class MinguoDateConverter extends AbstractChronoLocalDateConverter<MinguoEra> {
   @Override
   public boolean canConvert(Class type) {
      return MinguoDate.class == type;
   }

   @Override
   public Object fromString(String str) {
      return this.parseChronoLocalDate(str, "Minguo", Collections.singleton(MinguoChronology.INSTANCE));
   }

   protected ChronoLocalDate chronoLocalDateOf(MinguoEra era, int prolepticYear, int month, int dayOfMonth) {
      return MinguoDate.of(prolepticYear, month, dayOfMonth);
   }

   protected MinguoEra eraOf(String id) {
      return MinguoEra.valueOf(id);
   }
}
