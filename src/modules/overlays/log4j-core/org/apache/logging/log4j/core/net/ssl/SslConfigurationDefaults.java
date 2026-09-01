package org.apache.logging.log4j.core.net.ssl;

import java.security.KeyStore;

public class SslConfigurationDefaults {
   public static final String KEYSTORE_TYPE = KeyStore.getDefaultType();
   public static final String PROTOCOL = "TLS";
}
