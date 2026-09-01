package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Plugin;

@Lookup
@Plugin("upper")
public class UpperLookup implements StrLookup {
   @Override
   public String lookup(final String key) {
      return key != null ? key.toUpperCase() : null;
   }

   @Override
   public String lookup(final LogEvent event, final String key) {
      return this.lookup(key);
   }
}
