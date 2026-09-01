package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ContextDataInjectorFactory;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

@Lookup
@Plugin("ctx")
public class ContextMapLookup implements StrLookup {
   private final ContextDataInjector injector = ContextDataInjectorFactory.createInjector();

   @Override
   public String lookup(final String key) {
      return (String)this.currentContextData().getValue(key);
   }

   private ReadOnlyStringMap currentContextData() {
      return this.injector.rawContextData();
   }

   @Override
   public String lookup(final LogEvent event, final String key) {
      return event == null ? null : (String)event.getContextData().getValue(key);
   }
}
