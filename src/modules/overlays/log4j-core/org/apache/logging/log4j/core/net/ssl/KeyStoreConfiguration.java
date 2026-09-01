package org.apache.logging.log4j.core.net.ssl;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import javax.net.ssl.KeyManagerFactory;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin("KeyStore")
public class KeyStoreConfiguration extends AbstractKeyStoreConfiguration {
   private final String keyManagerFactoryAlgorithm;

   public KeyStoreConfiguration(
      final String location, final PasswordProvider passwordProvider, final String keyStoreType, final String keyManagerFactoryAlgorithm
   ) throws StoreConfigurationException {
      super(location, passwordProvider, keyStoreType);
      this.keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm == null ? KeyManagerFactory.getDefaultAlgorithm() : keyManagerFactoryAlgorithm;
   }

   @Deprecated
   public KeyStoreConfiguration(final String location, final char[] password, final String keyStoreType, final String keyManagerFactoryAlgorithm) throws StoreConfigurationException {
      this(location, new MemoryPasswordProvider(password), keyStoreType, keyManagerFactoryAlgorithm);
      if (password != null) {
         Arrays.fill(password, '\u0000');
      }
   }

   @Deprecated
   public KeyStoreConfiguration(final String location, final String password, final String keyStoreType, final String keyManagerFactoryAlgorithm) throws StoreConfigurationException {
      this(location, new MemoryPasswordProvider(password == null ? null : password.toCharArray()), keyStoreType, keyManagerFactoryAlgorithm);
   }

   @PluginFactory
   public static KeyStoreConfiguration createKeyStoreConfiguration(
      @PluginAttribute final String location,
      @PluginAttribute(sensitive = true) final char[] password,
      @PluginAttribute final String passwordEnvironmentVariable,
      @PluginAttribute final String passwordFile,
      @PluginAttribute("type") final String keyStoreType,
      @PluginAttribute final String keyManagerFactoryAlgorithm
   ) throws StoreConfigurationException {
      if (password != null && passwordEnvironmentVariable != null && passwordFile != null) {
         throw new StoreConfigurationException("You MUST set only one of 'password', 'passwordEnvironmentVariable' or 'passwordFile'.");
      }

      try {
         PasswordProvider provider = passwordFile != null
            ? new FilePasswordProvider(passwordFile)
            : (passwordEnvironmentVariable != null ? new EnvironmentPasswordProvider(passwordEnvironmentVariable) : new MemoryPasswordProvider(password));
         if (password != null) {
            Arrays.fill(password, '\u0000');
         }

         return new KeyStoreConfiguration(location, provider, keyStoreType, keyManagerFactoryAlgorithm);
      } catch (Exception ex) {
         throw new StoreConfigurationException("Could not configure KeyStore", ex);
      }
   }

   public KeyManagerFactory initKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
      KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(this.keyManagerFactoryAlgorithm);
      char[] password = this.getPassword();

      try {
         kmFactory.init(this.getKeyStore(), password != null ? password : DEFAULT_PASSWORD);
      } finally {
         if (password != null) {
            Arrays.fill(password, '\u0000');
         }
      }

      return kmFactory;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = super.hashCode();
      return 31 * result + (this.keyManagerFactoryAlgorithm == null ? 0 : this.keyManagerFactoryAlgorithm.hashCode());
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

      KeyStoreConfiguration other = (KeyStoreConfiguration)obj;
      if (this.keyManagerFactoryAlgorithm == null) {
         if (other.keyManagerFactoryAlgorithm != null) {
            return false;
         }
      } else if (!this.keyManagerFactoryAlgorithm.equals(other.keyManagerFactoryAlgorithm)) {
         return false;
      }

      return true;
   }

   public String getKeyManagerFactoryAlgorithm() {
      return this.keyManagerFactoryAlgorithm;
   }
}
