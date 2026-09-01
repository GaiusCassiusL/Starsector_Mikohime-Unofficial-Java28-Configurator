package org.apache.logging.log4j.core.lookup;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

@Lookup
@Plugin("log4j")
public class Log4jLookup extends AbstractConfigurationAwareLookup {
   public static final String KEY_CONFIG_LOCATION = "configLocation";
   public static final String KEY_CONFIG_PARENT_LOCATION = "configParentLocation";
   private static final Logger LOGGER = StatusLogger.getLogger();

   private static String asPath(final URI uri) {
      return uri.getScheme() != null && !uri.getScheme().equals("file") ? uri.toString() : uri.getPath();
   }

   private static URI getParent(final URI uri) throws URISyntaxException {
      String s = uri.toString();
      int offset = s.lastIndexOf(47);
      return offset > -1 ? new URI(s.substring(0, offset)) : new URI("../");
   }

   @Override
   public String lookup(final LogEvent event, final String key) {
      if (this.configuration != null) {
         ConfigurationSource configSrc = this.configuration.getConfigurationSource();
         File file = configSrc.getFile();
         if (file != null) {
            switch (key) {
               case "configLocation":
                  return file.getAbsolutePath();
               case "configParentLocation":
                  return file.getParentFile().getAbsolutePath();
               default:
                  return null;
            }
         }

         URL url = configSrc.getURL();
         if (url != null) {
            try {
               switch (key) {
                  case "configLocation":
                     return asPath(url.toURI());
                  case "configParentLocation":
                     return asPath(getParent(url.toURI()));
                  default:
                     return null;
               }
            } catch (URISyntaxException use) {
               LOGGER.error(use);
               return null;
            }
         }
      }

      return null;
   }
}
