package org.apache.logging.log4j.core.config;

public class DefaultConfiguration extends AbstractConfiguration {
   public static final String DEFAULT_NAME = "Default";
   public static final String DEFAULT_PATTERN = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

   public DefaultConfiguration() {
      super(null, ConfigurationSource.NULL_SOURCE);
      this.setToDefault();
   }

   @Override
   protected void doConfigure() {
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName();
   }
}
