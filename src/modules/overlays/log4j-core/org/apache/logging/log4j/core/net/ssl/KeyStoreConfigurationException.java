package org.apache.logging.log4j.core.net.ssl;

public class KeyStoreConfigurationException extends StoreConfigurationException {
   private static final long serialVersionUID = 1L;

   public KeyStoreConfigurationException(final Exception e) {
      super(e);
   }
}
