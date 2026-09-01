package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.spi.Provider;

public class Log4jProvider extends Provider {
   public Log4jProvider() {
      super(10, "3.0.0", Log4jContextFactory.class);
   }
}
