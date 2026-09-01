package org.apache.logging.log4j.core.config.properties;

import java.io.IOException;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class PropertiesConfiguration extends BuiltConfiguration implements Reconfigurable {
   public PropertiesConfiguration(final LoggerContext loggerContext, final ConfigurationSource source, final Component root) {
      super(loggerContext, source, root);
   }

   @Override
   public Configuration reconfigure() {
      try {
         ConfigurationSource source = this.getConfigurationSource().resetInputStream();
         if (source == null) {
            return null;
         }

         PropertiesConfigurationFactory factory = new PropertiesConfigurationFactory();
         PropertiesConfiguration config = factory.getConfiguration(this.getLoggerContext(), source);
         return config != null && config.getState() == LifeCycle.State.INITIALIZING ? config : null;
      } catch (IOException ex) {
         LOGGER.error("Cannot locate file {}: {}", this.getConfigurationSource(), ex);
         return null;
      }
   }
}
