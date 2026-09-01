package org.apache.logging.log4j.core.config;

import java.net.URI;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.BasicAuthorizationProvider;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.PropertyKey;

public abstract class ConfigurationFactory extends ConfigurationBuilderFactory {
   public static final PropertyKey LOG4J1_CONFIGURATION_FILE_PROPERTY = Log4jPropertyKey.CONFIG_V1_FILE_NAME;
   public static final PropertyKey LOG4J1_EXPERIMENTAL = Log4jPropertyKey.CONFIG_V1_COMPATIBILITY_ENABLED;
   public static final String NAMESPACE = "ConfigurationFactory";
   public static final Key<ConfigurationFactory> KEY = new Key<ConfigurationFactory>() {};
   public static final Key<PluginNamespace> PLUGIN_CATEGORY_KEY = new Key<PluginNamespace>() {};
   protected static final Logger LOGGER = StatusLogger.getLogger();
   protected static final String TEST_PREFIX = "log4j2-test";
   protected static final String DEFAULT_PREFIX = "log4j2";
   protected static final String LOG4J1_VERSION = "1";
   protected static final String LOG4J2_VERSION = "2";
   private static final String CLASS_LOADER_SCHEME = "classloader";
   private static final String CLASS_PATH_SCHEME = "classpath";
   private static final String[] PREFIXES = new String[]{"log4j2.", "log4j2.Configuration."};
   protected StrSubstitutor substitutor;

   @Deprecated(since = "3.0.0", forRemoval = true)
   public static ConfigurationFactory getInstance() {
      return (ConfigurationFactory)LoggerContext.getContext(false).getInjector().getInstance(KEY);
   }

   public static AuthorizationProvider authorizationProvider(final PropertyEnvironment props) {
      String authClass = props.getStringProperty(Log4jPropertyKey.CONFIG_AUTH_PROVIDER);
      AuthorizationProvider provider = null;
      if (authClass != null) {
         try {
            Object obj = LoaderUtil.newInstanceOf(authClass);
            if (obj instanceof AuthorizationProvider) {
               provider = (AuthorizationProvider)obj;
            } else {
               LOGGER.warn("{} is not an AuthorizationProvider, using default", obj.getClass().getName());
            }
         } catch (Exception ex) {
            LOGGER.warn("Unable to create {}, using default: {}", authClass, ex.getMessage());
         }
      }

      if (provider == null) {
         provider = new BasicAuthorizationProvider(props);
      }

      return provider;
   }

   @Inject
   public void setSubstitutor(final StrSubstitutor substitutor) {
      this.substitutor = substitutor;
   }

   protected abstract String[] getSupportedTypes();

   protected String getTestPrefix() {
      return "log4j2-test";
   }

   protected String getDefaultPrefix() {
      return "log4j2";
   }

   protected String getVersion() {
      return "2";
   }

   protected boolean isActive() {
      return true;
   }

   public abstract Configuration getConfiguration(final LoggerContext loggerContext, ConfigurationSource source);

   public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
      if (!this.isActive()) {
         return null;
      }

      if (configLocation != null) {
         ConfigurationSource source = ConfigurationSource.fromUri(configLocation);
         if (source != null) {
            return this.getConfiguration(loggerContext, source);
         }
      }

      return null;
   }

   public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation, final ClassLoader loader) {
      if (!this.isActive()) {
         return null;
      }

      if (loader == null) {
         return this.getConfiguration(loggerContext, name, configLocation);
      }

      if (isClassLoaderUri(configLocation)) {
         String path = extractClassLoaderUriPath(configLocation);
         ConfigurationSource source = ConfigurationSource.fromResource(path, loader);
         if (source != null) {
            Configuration configuration = this.getConfiguration(loggerContext, source);
            if (configuration != null) {
               return configuration;
            }
         }
      }

      return this.getConfiguration(loggerContext, name, configLocation);
   }

   static boolean isClassLoaderUri(final URI uri) {
      if (uri == null) {
         return false;
      }

      String scheme = uri.getScheme();
      return scheme == null || scheme.equals("classloader") || scheme.equals("classpath");
   }

   static String extractClassLoaderUriPath(final URI uri) {
      return uri.getScheme() == null ? uri.getPath() : uri.getSchemeSpecificPart();
   }
}
