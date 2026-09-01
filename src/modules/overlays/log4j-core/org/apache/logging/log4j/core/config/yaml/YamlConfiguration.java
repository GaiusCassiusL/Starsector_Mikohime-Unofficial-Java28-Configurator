package org.apache.logging.log4j.core.config.yaml;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.json.JsonConfiguration;

public class YamlConfiguration extends JsonConfiguration {
   public YamlConfiguration(final LoggerContext loggerContext, final ConfigurationSource configSource) {
      super(loggerContext, configSource);
   }

   @Override
   protected ObjectMapper getObjectMapper() {
      return new ObjectMapper(new YAMLFactory()).configure(Feature.ALLOW_COMMENTS, true);
   }

   @Override
   public Configuration reconfigure() {
      try {
         ConfigurationSource source = this.getConfigurationSource().resetInputStream();
         return source == null ? null : new YamlConfiguration(this.getLoggerContext(), source);
      } catch (IOException ex) {
         LOGGER.error("Cannot locate file {}", this.getConfigurationSource(), ex);
         return null;
      }
   }
}
