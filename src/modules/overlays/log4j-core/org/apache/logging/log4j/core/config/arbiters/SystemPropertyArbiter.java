package org.apache.logging.log4j.core.config.arbiters;

import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;

@Configurable(elementType = "Arbiter", printObject = true, deferChildren = true)
@Plugin
public final class SystemPropertyArbiter implements Arbiter {
   private final String propertyName;
   private final String propertyValue;

   private SystemPropertyArbiter(final String propertyName, final String propertyValue) {
      this.propertyName = propertyName;
      this.propertyValue = propertyValue;
   }

   @Override
   public boolean isCondition() {
      String value = System.getProperty(this.propertyName);
      return value != null && (this.propertyValue == null || value.equals(this.propertyValue));
   }

   @PluginBuilderFactory
   public static SystemPropertyArbiter.Builder newBuilder() {
      return new SystemPropertyArbiter.Builder();
   }

   public static class Builder implements org.apache.logging.log4j.core.util.Builder<SystemPropertyArbiter> {
      public static final String ATTR_PROPERTY_NAME = "propertyName";
      public static final String ATTR_PROPERTY_VALUE = "propertyValue";
      @PluginBuilderAttribute("propertyName")
      private String propertyName;
      @PluginBuilderAttribute("propertyValue")
      private String propertyValue;

      public SystemPropertyArbiter.Builder setPropertyName(final String propertyName) {
         this.propertyName = propertyName;
         return this.asBuilder();
      }

      public SystemPropertyArbiter.Builder setPropertyValue(final String propertyValue) {
         this.propertyValue = propertyValue;
         return this.asBuilder();
      }

      public SystemPropertyArbiter.Builder asBuilder() {
         return this;
      }

      public SystemPropertyArbiter build() {
         return new SystemPropertyArbiter(this.propertyName, this.propertyValue);
      }
   }
}
