package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(printObject = true)
@Plugin
public final class IfAccumulatedFileCount implements PathCondition {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final int threshold;
   private int count;
   private final PathCondition[] nestedConditions;

   private IfAccumulatedFileCount(final int thresholdParam, final PathCondition... nestedConditions) {
      if (thresholdParam <= 0) {
         throw new IllegalArgumentException("Count must be a positive integer but was " + thresholdParam);
      }

      this.threshold = thresholdParam;
      this.nestedConditions = PathCondition.copy(nestedConditions);
   }

   public int getThresholdCount() {
      return this.threshold;
   }

   public List<PathCondition> getNestedConditions() {
      return Collections.unmodifiableList(Arrays.asList(this.nestedConditions));
   }

   @Override
   public boolean accept(final Path basePath, final Path relativePath, final BasicFileAttributes attrs) {
      boolean result = ++this.count > this.threshold;
      String match = result ? ">" : "<=";
      String accept = result ? "ACCEPTED" : "REJECTED";
      LOGGER.trace("IfAccumulatedFileCount {}: {} count '{}' {} threshold '{}'", accept, relativePath, this.count, match, this.threshold);
      return result ? IfAll.accept(this.nestedConditions, basePath, relativePath, attrs) : result;
   }

   @Override
   public void beforeFileTreeWalk() {
      this.count = 0;
      IfAll.beforeFileTreeWalk(this.nestedConditions);
   }

   @PluginFactory
   public static IfAccumulatedFileCount createFileCountCondition(
      @PluginAttribute(value = "exceeds", defaultInt = Integer.MAX_VALUE) final int threshold,
      @PluginElement("PathConditions") final PathCondition... nestedConditions
   ) {
      if (threshold == Integer.MAX_VALUE) {
         LOGGER.error("IfAccumulatedFileCount invalid or missing threshold value.");
      }

      return new IfAccumulatedFileCount(threshold, nestedConditions);
   }

   @Override
   public String toString() {
      String nested = this.nestedConditions.length == 0 ? "" : " AND " + Arrays.toString(this.nestedConditions);
      return "IfAccumulatedFileCount(exceeds=" + this.threshold + nested + ")";
   }
}
