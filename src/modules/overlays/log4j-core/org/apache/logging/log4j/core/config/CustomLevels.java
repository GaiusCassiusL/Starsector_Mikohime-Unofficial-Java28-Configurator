package org.apache.logging.log4j.core.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin
public final class CustomLevels {
   private final List<CustomLevelConfig> customLevels;

   private CustomLevels(final CustomLevelConfig[] customLevels) {
      this.customLevels = new ArrayList<>(Arrays.asList(customLevels));
   }

   @PluginFactory
   public static CustomLevels createCustomLevels(@PluginElement("CustomLevels") final CustomLevelConfig[] customLevels) {
      return new CustomLevels(customLevels == null ? new CustomLevelConfig[0] : customLevels);
   }

   public List<CustomLevelConfig> getCustomLevels() {
      return this.customLevels;
   }
}
