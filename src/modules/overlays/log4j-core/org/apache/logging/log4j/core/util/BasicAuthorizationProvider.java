package org.apache.logging.log4j.core.util;

import java.net.URLConnection;
import java.util.Base64;
import java.util.Base64.Encoder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;

public class BasicAuthorizationProvider implements AuthorizationProvider {
   private static final String[] PREFIXES = new String[]{"log4j2.config.", "log4j2.Configuration.", "logging.auth."};
   private static final String AUTH_USER_NAME = "username";
   private static final String AUTH_PASSWORD = "password";
   private static final String AUTH_PASSWORD_DECRYPTOR = "passwordDecryptor";
   public static final String CONFIG_USER_NAME = "log4j2.configurationUserName";
   public static final String CONFIG_PASSWORD = "log4j2.configurationPassword";
   public static final String PASSWORD_DECRYPTOR = "log4j2.passwordDecryptor";
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final Encoder encoder = Base64.getEncoder();
   private String authString = null;

   public BasicAuthorizationProvider(final PropertyEnvironment props) {
      String userName = props.getStringProperty(PREFIXES, "username", () -> props.getStringProperty("log4j2.configurationUserName"));
      String password = props.getStringProperty(PREFIXES, "password", () -> props.getStringProperty("log4j2.configurationPassword"));
      String decryptor = props.getStringProperty(PREFIXES, "passwordDecryptor", () -> props.getStringProperty("log4j2.passwordDecryptor"));
      if (decryptor != null) {
         try {
            Object obj = LoaderUtil.newInstanceOf(decryptor);
            if (obj instanceof PasswordDecryptor) {
               password = ((PasswordDecryptor)obj).decryptPassword(password);
            }
         } catch (Exception ex) {
            LOGGER.warn("Unable to decrypt password.", ex);
         }
      }

      if (userName != null && password != null) {
         this.authString = "Basic " + encoder.encodeToString((userName + ":" + password).getBytes());
      }
   }

   @Override
   public void addAuthorization(final URLConnection urlConnection) {
      if (this.authString != null) {
         urlConnection.setRequestProperty("Authorization", this.authString);
      }
   }
}
