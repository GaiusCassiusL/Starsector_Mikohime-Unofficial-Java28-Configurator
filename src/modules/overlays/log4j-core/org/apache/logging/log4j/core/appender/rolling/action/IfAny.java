package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin
public final class IfAny implements PathCondition {
   private final PathCondition[] components;

   private IfAny(final PathCondition... filters) {
      this.components = Objects.requireNonNull(filters, "filters");
   }

   public PathCondition[] getDeleteFilters() {
      return this.components;
   }

   @Override
   public boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
      for (PathCondition component : this.components) {
         if (component.accept(baseDir, relativePath, attrs)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void beforeFileTreeWalk() {
      for (PathCondition condition : this.components) {
         condition.beforeFileTreeWalk();
      }
   }

   @PluginFactory
   public static IfAny createOrCondition(@PluginElement("PathConditions") final PathCondition... components) {
      return new IfAny(components);
   }

   @Override
   public String toString() {
      return "IfAny" + Arrays.toString(this.components);
   }
}
