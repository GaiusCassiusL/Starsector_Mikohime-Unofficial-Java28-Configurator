package org.apache.logging.log4j.core.time.internal;

import java.util.concurrent.locks.LockSupport;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.util.Lazy;

public final class CachedClock implements Clock {
   private static final int UPDATE_THRESHOLD = 1000;
   private static final Lazy<CachedClock> INSTANCE = Lazy.lazy(CachedClock::new);
   private volatile long millis = System.currentTimeMillis();
   private short count = 0;

   private CachedClock() {
      Thread updater = new Log4jThread(() -> {
         while (true) {
            this.millis = System.currentTimeMillis();
            LockSupport.parkNanos(1000000L);
         }
      }, "CachedClock Updater Thread");
      updater.setDaemon(true);
      updater.start();
   }

   @Factory
   public static CachedClock instance() {
      return (CachedClock)INSTANCE.value();
   }

   @Override
   public long currentTimeMillis() {
      if (++this.count > 1000) {
         this.millis = System.currentTimeMillis();
         this.count = 0;
      }

      return this.millis;
   }
}
