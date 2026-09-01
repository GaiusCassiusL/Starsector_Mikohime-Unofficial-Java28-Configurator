package org.apache.logging.log4j.core.time.internal;

import java.util.concurrent.locks.LockSupport;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.util.Lazy;

public final class CoarseCachedClock implements Clock {
   private static final Lazy<CoarseCachedClock> INSTANCE = Lazy.lazy(CoarseCachedClock::new);
   private volatile long millis = System.currentTimeMillis();

   private CoarseCachedClock() {
      Thread updater = new Log4jThread("CoarseCachedClock Updater Thread") {
         @Override
         public void run() {
            while (true) {
               CoarseCachedClock.this.millis = System.currentTimeMillis();
               LockSupport.parkNanos(1000000L);
            }
         }
      };
      updater.setDaemon(true);
      updater.start();
   }

   public static CoarseCachedClock instance() {
      return (CoarseCachedClock)INSTANCE.value();
   }

   @Override
   public long currentTimeMillis() {
      return this.millis;
   }
}
