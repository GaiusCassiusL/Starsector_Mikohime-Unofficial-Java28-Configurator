package com.thoughtworks.xstream.converters.time;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.HijrahEra;
import java.util.HashSet;
import java.util.Set;

public class HijrahDateConverter extends AbstractChronoLocalDateConverter<HijrahEra> {
   private final Set<Chronology> hijrahChronologies = new HashSet<>();

   public HijrahDateConverter() {
      for (Chronology chronology : Chronology.getAvailableChronologies()) {
         if (chronology instanceof HijrahChronology) {
            this.hijrahChronologies.add(chronology);
         }
      }
   }

   @Override
   public boolean canConvert(Class type) {
      return HijrahDate.class == type;
   }

   @Override
   public Object fromString(String str) {
      return this.parseChronoLocalDate(str, "Hijrah", this.hijrahChronologies);
   }

   protected ChronoLocalDate chronoLocalDateOf(HijrahEra era, int prolepticYear, int month, int dayOfMonth) {
      return era != null ? HijrahDate.of(prolepticYear, month, dayOfMonth) : null;
   }

   protected HijrahEra eraOf(String id) {
      return HijrahEra.valueOf(id);
   }
}
