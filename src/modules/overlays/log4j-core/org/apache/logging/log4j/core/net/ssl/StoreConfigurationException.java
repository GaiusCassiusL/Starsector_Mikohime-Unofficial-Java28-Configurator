package org.apache.logging.log4j.core.net.ssl;

public class StoreConfigurationException extends Exception {
   private static final long serialVersionUID = 1L;

   public StoreConfigurationException(final Exception e) {
      super(e);
   }

   public StoreConfigurationException(final String message) {
      super(message);
   }

   public StoreConfigurationException(final String message, final Exception e) {
      super(message, e);
   }
}
