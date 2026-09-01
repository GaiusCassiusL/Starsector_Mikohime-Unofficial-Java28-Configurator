package org.apache.logging.log4j.core.net.ssl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.util.NetUtils;

public class AbstractKeyStoreConfiguration extends StoreConfiguration<KeyStore> {
   static final char[] DEFAULT_PASSWORD = "changeit".toCharArray();
   private final KeyStore keyStore;
   private final String keyStoreType;

   public AbstractKeyStoreConfiguration(final String location, final PasswordProvider passwordProvider, final String keyStoreType) throws StoreConfigurationException {
      super(location, passwordProvider);
      this.keyStoreType = keyStoreType == null ? SslConfigurationDefaults.KEYSTORE_TYPE : keyStoreType;
      this.keyStore = this.load();
   }

   protected KeyStore load() throws StoreConfigurationException {
      String loadLocation = this.getLocation();
      LOGGER.debug("Loading keystore from location {}", loadLocation);

      try {
         if (loadLocation == null) {
            throw new IOException("The location is null");
         }

         try (InputStream fin = this.openInputStream(loadLocation)) {
            KeyStore ks = KeyStore.getInstance(this.keyStoreType);
            char[] password = this.getPassword();

            try {
               ks.load(fin, password != null ? password : DEFAULT_PASSWORD);
            } finally {
               if (password != null) {
                  Arrays.fill(password, '\u0000');
               }
            }

            LOGGER.debug("KeyStore successfully loaded from location {}", loadLocation);
            return ks;
         }
      } catch (CertificateException e) {
         LOGGER.error("No Provider supports a KeyStoreSpi implementation for the specified type {} for location {}", this.keyStoreType, loadLocation, e);
         throw new StoreConfigurationException(loadLocation, e);
      } catch (NoSuchAlgorithmException e) {
         LOGGER.error("The algorithm used to check the integrity of the keystore cannot be found for location {}", loadLocation, e);
         throw new StoreConfigurationException(loadLocation, e);
      } catch (KeyStoreException e) {
         LOGGER.error("KeyStoreException for location {}", loadLocation, e);
         throw new StoreConfigurationException(loadLocation, e);
      } catch (FileNotFoundException e) {
         LOGGER.error("The keystore file {} is not found", loadLocation, e);
         throw new StoreConfigurationException(loadLocation, e);
      } catch (IOException e) {
         LOGGER.error("Something is wrong with the format of the keystore or the given password for location {}", loadLocation, e);
         throw new StoreConfigurationException(loadLocation, e);
      }
   }

   private InputStream openInputStream(final String filePathOrUri) {
      return ConfigurationSource.fromUri(NetUtils.toURI(filePathOrUri)).getInputStream();
   }

   public KeyStore getKeyStore() {
      return this.keyStore;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = super.hashCode();
      result = 31 * result + (this.keyStore == null ? 0 : this.keyStore.hashCode());
      return 31 * result + (this.keyStoreType == null ? 0 : this.keyStoreType.hashCode());
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }

      if (!super.equals(obj)) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      AbstractKeyStoreConfiguration other = (AbstractKeyStoreConfiguration)obj;
      if (this.keyStore == null) {
         if (other.keyStore != null) {
            return false;
         }
      } else if (!this.keyStore.equals(other.keyStore)) {
         return false;
      }

      if (this.keyStoreType == null) {
         if (other.keyStoreType != null) {
            return false;
         }
      } else if (!this.keyStoreType.equals(other.keyStoreType)) {
         return false;
      }

      return true;
   }

   public String getKeyStoreType() {
      return this.keyStoreType;
   }
}
