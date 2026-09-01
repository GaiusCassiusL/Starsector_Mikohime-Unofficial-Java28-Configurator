package org.apache.logging.log4j.core.time;

public final class SystemNanoClock implements NanoClock {
   @Override
   public long nanoTime() {
      return System.nanoTime();
   }
}
