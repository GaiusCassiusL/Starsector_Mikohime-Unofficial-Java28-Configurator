package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Plugin;

@Lookup
@Plugin("lower")
public class LowerLookup implements StrLookup {
   @Override
   public String lookup(final String key) {
      return key != null ? key.toLowerCase() : null;
   }

   @Override
   public String lookup(final LogEvent event, final String key) {
      return this.lookup(key);
   }
}
