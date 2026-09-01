package org.apache.logging.log4j.core.time.internal;

import java.time.Instant;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.time.PreciseClock;
import org.apache.logging.log4j.util.PropertiesUtil;

public final class SystemClock implements Clock, PreciseClock {
   private final boolean usePreciseClock = PropertiesUtil.getProperties().getBooleanProperty(Log4jPropertyKey.USE_PRECISE_CLOCK, false);

   @Override
   public long currentTimeMillis() {
      return System.currentTimeMillis();
   }

   @Override
   public void init(final MutableInstant mutableInstant) {
      if (this.usePreciseClock) {
         Instant instant = java.time.Clock.systemUTC().instant();
         mutableInstant.initFromEpochSecond(instant.getEpochSecond(), instant.getNano());
      } else {
         mutableInstant.initFromEpochMilli(System.currentTimeMillis(), 0);
      }
   }
}
