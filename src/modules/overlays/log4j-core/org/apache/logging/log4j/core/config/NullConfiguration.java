package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;

public class NullConfiguration extends AbstractConfiguration {
   public static final String NULL_NAME = "Null";

   public NullConfiguration() {
      super(null, ConfigurationSource.NULL_SOURCE);
      this.setName("Null");
      LoggerConfig root = this.getRootLogger();
      root.setLevel(Level.OFF);
   }
}
