package org.apache.logging.log4j.core.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.Strings;

public class DefaultConfigurationFactory extends ConfigurationFactory {
   private static final String ALL_TYPES = "*";
   private static final String OVERRIDE_PARAM = "override";
   private final Lazy<List<ConfigurationFactory>> configurationFactories;

   @Inject
   public DefaultConfigurationFactory(final Injector injector) {
      this.configurationFactories = Lazy.lazy(() -> loadConfigurationFactories(injector));
   }

   @Override
   public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
      if (configLocation == null) {
         PropertyEnvironment properties = loggerContext.getProperties();
         if (properties == null) {
            properties = PropertiesUtil.getProperties();
         }

         String configLocationStr = this.substitutor.replace(properties.getStringProperty(Log4jPropertyKey.CONFIG_LOCATION));
         if (configLocationStr != null) {
            String[] sources = this.parseConfigLocations(configLocationStr);
            if (sources.length > 1) {
               List<AbstractConfiguration> configs = new ArrayList<>();

               for (String sourceLocation : sources) {
                  Configuration config = this.getConfiguration(loggerContext, sourceLocation.trim());
                  if (config != null) {
                     if (!(config instanceof AbstractConfiguration)) {
                        LOGGER.error("Failed to created configuration at {}", sourceLocation);
                        return null;
                     }

                     configs.add((AbstractConfiguration)config);
                  } else {
                     LOGGER.warn("Unable to create configuration for {}, ignoring", sourceLocation);
                  }
               }

               if (configs.size() > 1) {
                  return new CompositeConfiguration(configs);
               }

               if (configs.size() == 1) {
                  return configs.get(0);
               }
            }

            return this.getConfiguration(loggerContext, configLocationStr);
         }

         String log4j1ConfigStr = this.substitutor.replace(properties.getStringProperty(LOG4J1_CONFIGURATION_FILE_PROPERTY));
         if (log4j1ConfigStr != null) {
            System.setProperty(LOG4J1_EXPERIMENTAL.getSystemKey(), "true");
            return this.getConfiguration("1", loggerContext, log4j1ConfigStr);
         }

         for (ConfigurationFactory factory : (List)this.configurationFactories.value()) {
            String[] types = factory.getSupportedTypes();
            if (types != null) {
               for (String type : types) {
                  if (type.equals("*")) {
                     Configuration config = factory.getConfiguration(loggerContext, name, null);
                     if (config != null) {
                        return config;
                     }
                  }
               }
            }
         }
      } else {
         String[] sources = this.parseConfigLocations(configLocation);
         if (sources.length > 1) {
            List<AbstractConfiguration> configs = new ArrayList<>();

            for (String sourceLocation : sources) {
               Configuration config = this.getConfiguration(loggerContext, sourceLocation.trim());
               if (!(config instanceof AbstractConfiguration)) {
                  LOGGER.error("Failed to created configuration at {}", sourceLocation);
                  return null;
               }

               configs.add((AbstractConfiguration)config);
            }

            return new CompositeConfiguration(configs);
         }

         String configLocationStr = configLocation.toString();

         for (ConfigurationFactory factory : (List)this.configurationFactories.value()) {
            String[] types = factory.getSupportedTypes();
            if (types != null) {
               for (String type : types) {
                  if (type.equals("*") || configLocationStr.endsWith(type)) {
                     Configuration config = factory.getConfiguration(loggerContext, name, configLocation);
                     if (config != null) {
                        return config;
                     }
                  }
               }
            }
         }
      }

      Configuration config = this.getConfiguration(loggerContext, true, name);
      if (config == null) {
         config = this.getConfiguration(loggerContext, true, null);
         if (config == null) {
            config = this.getConfiguration(loggerContext, false, name);
            if (config == null) {
               config = this.getConfiguration(loggerContext, false, null);
            }
         }
      }

      if (config != null) {
         return config;
      }

      LOGGER.warn(
         "No Log4j 2 configuration file found. Using default configuration (logging only errors to the console), or user programmatically provided configurations. Set system property 'log4j2.*.{}' to show Log4j 2 internal initialization logging. See https://logging.apache.org/log4j/2.x/manual/configuration.html for instructions on how to configure Log4j 2",
         LoggingSystemProperty.STATUS_LOGGER_DEBUG
      );
      return new DefaultConfiguration();
   }

   private Configuration getConfiguration(final LoggerContext loggerContext, final String configLocationStr) {
      return this.getConfiguration(null, loggerContext, configLocationStr);
   }

   private Configuration getConfiguration(final String requiredVersion, final LoggerContext loggerContext, final String configLocationStr) {
      ConfigurationSource source = null;

      try {
         source = ConfigurationSource.fromUri(NetUtils.toURI(configLocationStr));
      } catch (Exception ex) {
         LOGGER.catching(Level.DEBUG, ex);
      }

      if (source != null) {
         for (ConfigurationFactory factory : (List)this.configurationFactories.value()) {
            if (requiredVersion == null || factory.getVersion().equals(requiredVersion)) {
               String[] types = factory.getSupportedTypes();
               if (types != null) {
                  for (String type : types) {
                     if (type.equals("*") || configLocationStr.endsWith(type)) {
                        Configuration config = factory.getConfiguration(loggerContext, source);
                        if (config != null) {
                           return config;
                        }
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private Configuration getConfiguration(final LoggerContext loggerContext, final boolean isTest, final String name) {
      boolean named = Strings.isNotEmpty(name);
      ClassLoader loader = LoaderUtil.getThreadContextClassLoader();

      for (ConfigurationFactory factory : (List)this.configurationFactories.value()) {
         String prefix = isTest ? factory.getTestPrefix() : factory.getDefaultPrefix();
         String[] types = factory.getSupportedTypes();
         if (types != null) {
            for (String suffix : types) {
               if (!suffix.equals("*")) {
                  String configName = named ? prefix + name + suffix : prefix + suffix;
                  ConfigurationSource source = ConfigurationSource.fromResource(configName, loader);
                  if (source != null) {
                     if (!factory.isActive()) {
                        LOGGER.warn("Found configuration file {} for inactive ConfigurationFactory {}", configName, factory.getClass().getName());
                     }

                     return factory.getConfiguration(loggerContext, source);
                  }
               }
            }
         }
      }

      return null;
   }

   @Override
   public String[] getSupportedTypes() {
      return null;
   }

   @Override
   public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
      if (source != null) {
         String config = source.getLocation();

         for (ConfigurationFactory factory : (List)this.configurationFactories.value()) {
            String[] types = factory.getSupportedTypes();
            if (types != null) {
               for (String type : types) {
                  if (type.equals("*") || config != null && config.endsWith(type)) {
                     Configuration c = factory.getConfiguration(loggerContext, source);
                     if (c != null) {
                        LOGGER.debug("Loaded configuration from {}", source);
                        return c;
                     }

                     LOGGER.error("Cannot determine the ConfigurationFactory to use for {}", config);
                     return null;
                  }
               }
            }
         }
      }

      LOGGER.error("Cannot process configuration, input source is null");
      return null;
   }

   private String[] parseConfigLocations(final URI configLocations) {
      String[] uris = configLocations.toString().split("\\?");
      List<String> locations = new ArrayList<>();
      if (uris.length > 1) {
         locations.add(uris[0]);
         String[] pairs = configLocations.getQuery().split("&");

         for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : pair;
            if (key.equalsIgnoreCase("override")) {
               locations.add(URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            }
         }

         return locations.toArray(new String[0]);
      } else {
         return new String[]{uris[0]};
      }
   }

   private String[] parseConfigLocations(final String configLocations) {
      String[] uris = configLocations.split(",");
      if (uris.length > 1) {
         return uris;
      }

      try {
         return this.parseConfigLocations(new URI(configLocations));
      } catch (URISyntaxException ex) {
         LOGGER.warn("Error parsing URI {}", configLocations);
         return new String[]{configLocations};
      }
   }

   private static List<ConfigurationFactory> loadConfigurationFactories(final Injector injector) {
      List<ConfigurationFactory> factories = new ArrayList<>();
      Optional.ofNullable(PropertiesUtil.getProperties().getStringProperty(Log4jPropertyKey.CONFIG_CONFIGURATION_FACTORY_CLASS_NAME))
         .flatMap(DefaultConfigurationFactory::tryLoadFactoryClass)
         .map(clazz -> {
            try {
               return (ConfigurationFactory)injector.getInstance(clazz);
            } catch (Exception ex) {
               LOGGER.error("Unable to create instance of {}", clazz, ex);
               return null;
            }
         })
         .ifPresent(factories::add);
      List<Class<? extends ConfigurationFactory>> configurationFactoryPluginClasses = new ArrayList<>();
      ((PluginNamespace)injector.getInstance(PLUGIN_CATEGORY_KEY)).forEach(type -> {
         try {
            configurationFactoryPluginClasses.add(type.getPluginClass().asSubclass(ConfigurationFactory.class));
         } catch (Exception ex) {
            LOGGER.warn("Unable to add class {}", type.getPluginClass(), ex);
         }
      });
      configurationFactoryPluginClasses.sort(OrderComparator.getInstance());
      configurationFactoryPluginClasses.forEach(clazz -> {
         try {
            factories.add((ConfigurationFactory)injector.getInstance(clazz));
         } catch (Exception ex) {
            LOGGER.error("Unable to create instance of {}", clazz, ex);
         }
      });
      return factories;
   }

   private static Optional<Class<? extends ConfigurationFactory>> tryLoadFactoryClass(final String factoryClass) {
      try {
         return Optional.of(Loader.loadClass(factoryClass).asSubclass(ConfigurationFactory.class));
      } catch (Exception ex) {
         LOGGER.error("Unable to load ConfigurationFactory class {}", factoryClass, ex);
         return Optional.empty();
      }
   }
}
