package org.apache.logging.log4j.core.async;

import java.net.URI;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.BasicContextSelector;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.Injector;

@Singleton
public class BasicAsyncLoggerContextSelector extends BasicContextSelector {
   @Inject
   public BasicAsyncLoggerContextSelector(final Injector injector) {
      super(injector);
   }

   @Override
   protected LoggerContext createContext() {
      return new AsyncLoggerContext("AsyncDefault", null, (URI)null, this.injector);
   }
}
