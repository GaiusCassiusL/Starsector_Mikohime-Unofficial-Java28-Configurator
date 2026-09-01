package org.apache.logging.log4j.core.config;

import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(printObject = true)
@Plugin("CustomLevel")
public final class CustomLevelConfig {
   private final String levelName;
   private final int intLevel;

   private CustomLevelConfig(final String levelName, final int intLevel) {
      this.levelName = Objects.requireNonNull(levelName, "levelName is null");
      this.intLevel = intLevel;
   }

   @PluginFactory
   public static CustomLevelConfig createLevel(@PluginAttribute("name") final String levelName, @PluginAttribute final int intLevel) {
      StatusLogger.getLogger().debug("Creating CustomLevel(name='{}', intValue={})", levelName, intLevel);
      Level.forName(levelName, intLevel);
      return new CustomLevelConfig(levelName, intLevel);
   }

   public String getLevelName() {
      return this.levelName;
   }

   public int getIntLevel() {
      return this.intLevel;
   }

   @Override
   public int hashCode() {
      return this.intLevel ^ this.levelName.hashCode();
   }

   @Override
   public boolean equals(final Object object) {
      if (this == object) {
         return true;
      }

      if (!(object instanceof CustomLevelConfig)) {
         return false;
      }

      CustomLevelConfig other = (CustomLevelConfig)object;
      return this.intLevel == other.intLevel && this.levelName.equals(other.levelName);
   }

   @Override
   public String toString() {
      return "CustomLevel[name=" + this.levelName + ", intLevel=" + this.intLevel + "]";
   }
}
