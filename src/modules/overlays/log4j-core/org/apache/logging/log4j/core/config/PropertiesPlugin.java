package org.apache.logging.log4j.core.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.di.Key;

@Configurable(printObject = true)
@Plugin("properties")
public final class PropertiesPlugin {
   private PropertiesPlugin() {
   }

   @PluginFactory
   public static StrLookup configureSubstitutor(@PluginElement("Properties") final Property[] properties, @PluginConfiguration final Configuration config) {
      Map<String, String> map;
      if (properties == null) {
         map = config.getProperties();
      } else {
         map = new HashMap<>(config.getProperties());

         for (Property prop : properties) {
            map.put(prop.getName(), prop.getValue());
         }
      }

      return config.<InterpolatorFactory>getComponent(Key.forClass(InterpolatorFactory.class)).newInterpolator(new PropertiesLookup(map));
   }
}
