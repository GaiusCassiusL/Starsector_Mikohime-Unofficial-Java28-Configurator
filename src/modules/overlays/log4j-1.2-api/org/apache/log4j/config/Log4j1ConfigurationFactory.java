package org.apache.log4j.config;

import java.io.IOException;
import java.io.InputStream;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class Log4j1ConfigurationFactory extends ConfigurationFactory {
   private static final String[] SUFFIXES = new String[]{".properties"};

   public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
      ConfigurationBuilder<BuiltConfiguration> builder;
      try (InputStream configStream = source.getInputStream()) {
         builder = new Log4j1ConfigurationParser().buildConfigurationBuilder(configStream);
      } catch (IOException e) {
         throw new ConfigurationException("Unable to load " + source, e);
      }

      return (Configuration)builder.build();
   }

   protected String[] getSupportedTypes() {
      return SUFFIXES;
   }
}
