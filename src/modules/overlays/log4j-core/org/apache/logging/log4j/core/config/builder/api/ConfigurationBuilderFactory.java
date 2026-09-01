package org.apache.logging.log4j.core.config.builder.api;

import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;

public abstract class ConfigurationBuilderFactory {
   public static ConfigurationBuilder<BuiltConfiguration> newConfigurationBuilder() {
      return new DefaultConfigurationBuilder<>();
   }

   public static <T extends BuiltConfiguration> ConfigurationBuilder<T> newConfigurationBuilder(final Class<T> clazz) {
      return new DefaultConfigurationBuilder<>(clazz);
   }
}
