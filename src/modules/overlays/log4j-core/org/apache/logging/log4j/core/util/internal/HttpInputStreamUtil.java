package org.apache.logging.log4j.core.util.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.status.StatusLogger;

public final class HttpInputStreamUtil {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final int NOT_MODIFIED = 304;
   private static final int NOT_AUTHORIZED = 401;
   private static final int NOT_FOUND = 404;
   private static final int OK = 200;
   private static final int BUF_SIZE = 1024;

   public static HttpInputStreamUtil.Result getInputStream(final LastModifiedSource source, final AuthorizationProvider authorizationProvider) {
      HttpInputStreamUtil.Result result = new HttpInputStreamUtil.Result();

      try {
         long lastModified = source.getLastModified();
         HttpURLConnection connection = UrlConnectionFactory.createConnection(
            source.getURI().toURL(), lastModified, SslConfigurationFactory.getSslConfiguration(), authorizationProvider
         );
         connection.connect();

         try {
            int code = connection.getResponseCode();
            switch (code) {
               case 200:
                  try (InputStream is = connection.getInputStream()) {
                     source.setLastModified(connection.getLastModified());
                     LOGGER.debug(
                        "Content was modified for {}. previous lastModified: {}, new lastModified: {}",
                        source.toString(),
                        lastModified,
                        connection.getLastModified()
                     );
                     result.status = Status.SUCCESS;
                     result.inputStream = new ByteArrayInputStream(readStream(is));
                     return result;
                  } catch (IOException e) {
                     try (InputStream es = connection.getErrorStream()) {
                        LOGGER.info("Error accessing configuration at {}: {}", source.toString(), readStream(es));
                     } catch (IOException ioe) {
                        LOGGER.error("Error accessing configuration at {}: {}", source.toString(), e.getMessage());
                     }

                     throw new ConfigurationException("Unable to access " + source.toString(), e);
                  }
               case 304:
                  LOGGER.debug("Configuration not modified");
                  result.status = Status.NOT_MODIFIED;
                  return result;
               case 401:
                  throw new ConfigurationException("Authorization failed");
               case 404:
                  LOGGER.debug("Unable to access {}: Not Found", source.toString());
                  result.status = Status.NOT_FOUND;
                  return result;
               default:
                  if (code < 0) {
                     LOGGER.info("Invalid response code returned");
                  } else {
                     LOGGER.info("Unexpected response code returned {}", code);
                  }

                  throw new ConfigurationException("Unable to access " + source.toString());
            }
         } finally {
            connection.disconnect();
         }
      } catch (IOException e) {
         LOGGER.warn("Error accessing {}: {}", source.toString(), e.getMessage());
         throw new ConfigurationException("Unable to access " + source.toString(), e);
      }
   }

   public static byte[] readStream(final InputStream is) throws IOException {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];

      int length;
      while ((length = is.read(buffer)) != -1) {
         result.write(buffer, 0, length);
      }

      return result.toByteArray();
   }

   public static class Result {
      private InputStream inputStream;
      private Status status;

      public Result() {
      }

      public Result(final Status status) {
         this.status = status;
      }

      public InputStream getInputStream() {
         return this.inputStream;
      }

      public Status getStatus() {
         return this.status;
      }
   }
}
