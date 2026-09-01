package org.apache.logging.log4j.core.net.ssl;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;

public class SslConfigurationFactory {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final Lazy<SslConfiguration> SSL_CONFIGURATION = Lazy.lazy(() -> {
      PropertyEnvironment props = PropertiesUtil.getProperties();
      return createSslConfiguration(props);
   });

   static SslConfiguration createSslConfiguration(final PropertyEnvironment props) {
      KeyStoreConfiguration keyStoreConfiguration = null;
      TrustStoreConfiguration trustStoreConfiguration = null;
      String location = props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_TRUST_STORE_LOCATION);
      if (location != null) {
         String password = props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_TRUST_STORE_PASSWORD);
         char[] passwordChars = null;
         if (password != null) {
            passwordChars = password.toCharArray();
         }

         try {
            trustStoreConfiguration = TrustStoreConfiguration.createKeyStoreConfiguration(
               location,
               passwordChars,
               props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_ENV_VAR),
               props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_FILE),
               props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_TRUST_STORE_TYPE),
               props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM)
            );
         } catch (Exception ex) {
            LOGGER.warn("Unable to create trust store configuration due to: {} {}", ex.getClass().getName(), ex.getMessage());
         }
      }

      location = props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_KEY_STORE_LOCATION);
      if (location != null) {
         String password = props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_KEY_STORE_PASSWORD);
         char[] passwordChars = null;
         if (password != null) {
            passwordChars = password.toCharArray();
         }

         try {
            keyStoreConfiguration = KeyStoreConfiguration.createKeyStoreConfiguration(
               location,
               passwordChars,
               props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_KEY_STORE_PASSWORD_ENV_VAR),
               props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_KEY_STORE_PASSWORD_FILE),
               props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_KEY_STORE_TYPE),
               props.getStringProperty(Log4jPropertyKey.TRANSPORT_SECURITY_KEY_STORE_KEY_MANAGER_FACTORY_ALGORITHM)
            );
         } catch (Exception ex) {
            LOGGER.warn("Unable to create key store configuration due to: {} {}", ex.getClass().getName(), ex.getMessage());
         }
      }

      if (trustStoreConfiguration == null && keyStoreConfiguration == null) {
         return null;
      }

      boolean isVerifyHostName = props.getBooleanProperty(Log4jPropertyKey.TRANSPORT_SECURITY_VERIFY_HOST_NAME, false);
      return SslConfiguration.createSSLConfiguration(null, keyStoreConfiguration, trustStoreConfiguration, isVerifyHostName);
   }

   public static SslConfiguration getSslConfiguration() {
      return (SslConfiguration)SSL_CONFIGURATION.value();
   }
}
