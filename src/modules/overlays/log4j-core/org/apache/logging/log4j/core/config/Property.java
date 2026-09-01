package org.apache.logging.log4j.core.config;

import java.util.Objects;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.PluginValue;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

@Configurable(printObject = true)
@Plugin
public final class Property {
   public static final Property[] EMPTY_ARRAY = new Property[0];
   private final String name;
   private final String value;
   private final boolean valueNeedsLookup;

   private Property(final String name, final String value) {
      this.name = name;
      this.value = value;
      this.valueNeedsLookup = value != null && value.contains("${");
   }

   public String getName() {
      return this.name;
   }

   public String getValue() {
      return Objects.toString(this.value, "");
   }

   public boolean isValueNeedsLookup() {
      return this.valueNeedsLookup;
   }

   @PluginFactory
   public static Property createProperty(
      @PluginAttribute @Required(message = "Property name cannot be null") final String name, @PluginValue final String value
   ) {
      return new Property(name, value);
   }

   @Override
   public String toString() {
      return this.name + "=" + this.getValue();
   }
}
