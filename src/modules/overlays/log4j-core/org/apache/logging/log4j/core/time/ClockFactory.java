package org.apache.logging.log4j.core.time;

import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.util.Lazy;

public final class ClockFactory {
   private static final Lazy<Clock> FALLBACK = Lazy.lazy(() -> {
      Injector injector = DI.createInjector();
      injector.init();
      return (Clock)injector.getInstance(Clock.KEY);
   });

   @Deprecated
   public static Clock getClock() {
      return (Clock)FALLBACK.value();
   }
}
