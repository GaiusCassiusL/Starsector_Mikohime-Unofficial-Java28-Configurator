package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Plugin;

@Lookup
@Plugin("env")
public class EnvironmentLookup extends AbstractLookup {
   @Override
   public String lookup(final LogEvent event, final String key) {
      return key != null ? System.getenv(key) : null;
   }
}
