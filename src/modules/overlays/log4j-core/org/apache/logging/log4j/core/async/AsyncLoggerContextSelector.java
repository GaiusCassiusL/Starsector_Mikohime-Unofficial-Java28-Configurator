package org.apache.logging.log4j.core.async;

import java.net.URI;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.util.PropertiesUtil;

@Singleton
public class AsyncLoggerContextSelector extends ClassLoaderContextSelector {
   public static boolean isSelected() {
      return AsyncLoggerContextSelector.class.getName().equals(PropertiesUtil.getProperties().getStringProperty(Log4jPropertyKey.CONTEXT_SELECTOR_CLASS_NAME));
   }

   @Inject
   public AsyncLoggerContextSelector(final Injector injector) {
      super(injector);
   }

   @Override
   protected LoggerContext createContext(final String name, final URI configLocation, final Injector injector) {
      return new AsyncLoggerContext(name, null, configLocation, injector);
   }

   @Override
   protected String toContextMapKey(final ClassLoader loader) {
      return "AsyncContext@" + Integer.toHexString(System.identityHashCode(loader));
   }

   @Override
   protected String defaultContextName() {
      return "DefaultAsyncContext@" + Thread.currentThread().getName();
   }
}
