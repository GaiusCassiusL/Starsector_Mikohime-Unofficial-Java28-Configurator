package org.apache.log4j.config;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PropertiesUtil;

@Namespace("ConfigurationFactory")
@Plugin("Log4j1PropertiesConfigurationFactory")
@Order(2)
public class PropertiesConfigurationFactory extends ConfigurationFactory {
   static final String FILE_EXTENSION = ".properties";
   protected static final String TEST_PREFIX = "log4j-test";
   protected static final String DEFAULT_PREFIX = "log4j";

   protected String[] getSupportedTypes() {
      return !PropertiesUtil.getProperties().getBooleanProperty(ConfigurationFactory.LOG4J1_EXPERIMENTAL, Boolean.FALSE) ? null : new String[]{".properties"};
   }

   public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
      int interval = PropertiesUtil.getProperties().getIntegerProperty("log4j1.monitorInterval", 0);
      return new PropertiesConfiguration(loggerContext, source, interval);
   }

   protected String getTestPrefix() {
      return "log4j-test";
   }

   protected String getDefaultPrefix() {
      return "log4j";
   }

   protected String getVersion() {
      return "1";
   }
}
