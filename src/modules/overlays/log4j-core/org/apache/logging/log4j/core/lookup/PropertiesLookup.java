package org.apache.logging.log4j.core.lookup;

import java.util.Collections;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;

public final class PropertiesLookup implements StrLookup {
   private final Map<String, String> properties;

   public PropertiesLookup(final Map<String, String> properties) {
      this.properties = properties == null ? Collections.emptyMap() : properties;
   }

   public Map<String, String> getProperties() {
      return this.properties;
   }

   @Override
   public String lookup(final LogEvent event, final String key) {
      return this.lookup(key);
   }

   @Override
   public String lookup(final String key) {
      return key == null ? null : this.properties.get(key);
   }

   @Override
   public String toString() {
      return "PropertiesLookup{properties=" + this.properties + "}";
   }
}
