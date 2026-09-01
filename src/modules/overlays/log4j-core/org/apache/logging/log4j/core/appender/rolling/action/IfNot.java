package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin
public final class IfNot implements PathCondition {
   private final PathCondition negate;

   private IfNot(final PathCondition negate) {
      this.negate = Objects.requireNonNull(negate, "filter");
   }

   public PathCondition getWrappedFilter() {
      return this.negate;
   }

   @Override
   public boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
      return !this.negate.accept(baseDir, relativePath, attrs);
   }

   @Override
   public void beforeFileTreeWalk() {
      this.negate.beforeFileTreeWalk();
   }

   @PluginFactory
   public static IfNot createNotCondition(@PluginElement("PathConditions") final PathCondition condition) {
      return new IfNot(condition);
   }

   @Override
   public String toString() {
      return "IfNot(" + this.negate + ")";
   }
}
