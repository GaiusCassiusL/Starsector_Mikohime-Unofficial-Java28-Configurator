package org.apache.logging.log4j.core.time.internal;

import org.apache.logging.log4j.core.time.Clock;

public final class SystemMillisClock implements Clock {
   @Override
   public long currentTimeMillis() {
      return System.currentTimeMillis();
   }
}
