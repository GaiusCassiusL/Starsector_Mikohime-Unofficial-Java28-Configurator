package org.apache.logging.log4j.core.lookup;

public abstract class AbstractLookup implements StrLookup {
   @Override
   public String lookup(final String key) {
      return this.lookup(null, key);
   }
}
