package org.apache.logging.log4j.core.net.ssl;

import java.util.Arrays;

class MemoryPasswordProvider implements PasswordProvider {
   private final char[] password;

   public MemoryPasswordProvider(final char[] chars) {
      if (chars != null) {
         this.password = (char[])chars.clone();
      } else {
         this.password = null;
      }
   }

   @Override
   public char[] getPassword() {
      return this.password == null ? null : (char[])this.password.clone();
   }

   public void clearSecrets() {
      Arrays.fill(this.password, '\u0000');
   }
}
