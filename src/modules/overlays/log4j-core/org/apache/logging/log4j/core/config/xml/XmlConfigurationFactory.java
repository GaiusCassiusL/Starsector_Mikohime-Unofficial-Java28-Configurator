package org.apache.logging.log4j.core.config.xml;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("ConfigurationFactory")
@Plugin("XmlConfigurationFactory")
@Order(5)
public class XmlConfigurationFactory extends ConfigurationFactory {
   public static final String[] SUFFIXES = new String[]{".xml", "*"};

   @Override
   public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
      return new XmlConfiguration(loggerContext, source);
   }

   @Override
   public String[] getSupportedTypes() {
      return SUFFIXES;
   }
}
