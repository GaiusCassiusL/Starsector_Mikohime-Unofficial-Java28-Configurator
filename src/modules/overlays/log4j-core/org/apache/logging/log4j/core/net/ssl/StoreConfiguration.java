package org.apache.logging.log4j.core.net.ssl;

import java.util.Arrays;
import java.util.Objects;
import org.apache.logging.log4j.status.StatusLogger;

public class StoreConfiguration<T> {
   protected static final StatusLogger LOGGER = StatusLogger.getLogger();
   private String location;
   private PasswordProvider passwordProvider;

   public StoreConfiguration(final String location, final PasswordProvider passwordProvider) {
      this.location = location;
      this.passwordProvider = Objects.requireNonNull(passwordProvider, "passwordProvider");
   }

   public void clearSecrets() {
      this.location = null;
      this.passwordProvider = null;
   }

   public String getLocation() {
      return this.location;
   }

   public void setLocation(final String location) {
      this.location = location;
   }

   public char[] getPassword() {
      return this.passwordProvider.getPassword();
   }

   public void setPassword(final char[] password) {
      this.passwordProvider = new MemoryPasswordProvider(password);
   }

   protected T load() throws StoreConfigurationException {
      return null;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.location == null ? 0 : this.location.hashCode());
      return 31 * result + Arrays.hashCode(this.passwordProvider.getPassword());
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      StoreConfiguration<?> other = (StoreConfiguration<?>)obj;
      if (this.location == null) {
         if (other.location != null) {
            return false;
         }
      } else if (!this.location.equals(other.location)) {
         return false;
      }

      return Arrays.equals(this.passwordProvider.getPassword(), other.passwordProvider.getPassword());
   }
}
