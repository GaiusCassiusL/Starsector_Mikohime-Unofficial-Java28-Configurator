package org.apache.logging.log4j.core.net.ssl;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin("TrustStore")
public class TrustStoreConfiguration extends AbstractKeyStoreConfiguration {
   private final String trustManagerFactoryAlgorithm;

   public TrustStoreConfiguration(
      final String location, final PasswordProvider passwordProvider, final String keyStoreType, final String trustManagerFactoryAlgorithm
   ) throws StoreConfigurationException {
      super(location, passwordProvider, keyStoreType);
      this.trustManagerFactoryAlgorithm = trustManagerFactoryAlgorithm == null ? TrustManagerFactory.getDefaultAlgorithm() : trustManagerFactoryAlgorithm;
   }

   @Deprecated
   public TrustStoreConfiguration(final String location, final char[] password, final String keyStoreType, final String trustManagerFactoryAlgorithm) throws StoreConfigurationException {
      this(location, new MemoryPasswordProvider(password), keyStoreType, trustManagerFactoryAlgorithm);
      if (password != null) {
         Arrays.fill(password, '\u0000');
      }
   }

   @PluginFactory
   public static TrustStoreConfiguration createKeyStoreConfiguration(
      @PluginAttribute final String location,
      @PluginAttribute(sensitive = true) final char[] password,
      @PluginAttribute final String passwordEnvironmentVariable,
      @PluginAttribute final String passwordFile,
      @PluginAttribute("type") final String keyStoreType,
      @PluginAttribute final String trustManagerFactoryAlgorithm
   ) throws StoreConfigurationException {
      if (password != null && passwordEnvironmentVariable != null && passwordFile != null) {
         throw new IllegalStateException("You MUST set only one of 'password', 'passwordEnvironmentVariable' or 'passwordFile'.");
      }

      try {
         PasswordProvider provider = passwordFile != null
            ? new FilePasswordProvider(passwordFile)
            : (passwordEnvironmentVariable != null ? new EnvironmentPasswordProvider(passwordEnvironmentVariable) : new MemoryPasswordProvider(password));
         if (password != null) {
            Arrays.fill(password, '\u0000');
         }

         return new TrustStoreConfiguration(location, provider, keyStoreType, trustManagerFactoryAlgorithm);
      } catch (Exception ex) {
         throw new StoreConfigurationException("Could not configure TrustStore", ex);
      }
   }

   public TrustManagerFactory initTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException {
      TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(this.trustManagerFactoryAlgorithm);
      tmFactory.init(this.getKeyStore());
      return tmFactory;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = super.hashCode();
      return 31 * result + (this.trustManagerFactoryAlgorithm == null ? 0 : this.trustManagerFactoryAlgorithm.hashCode());
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

      TrustStoreConfiguration other = (TrustStoreConfiguration)obj;
      if (this.trustManagerFactoryAlgorithm == null) {
         if (other.trustManagerFactoryAlgorithm != null) {
            return false;
         }
      } else if (!this.trustManagerFactoryAlgorithm.equals(other.trustManagerFactoryAlgorithm)) {
         return false;
      }

      return true;
   }

   public String getTrustManagerFactoryAlgorithm() {
      return this.trustManagerFactoryAlgorithm;
   }
}
