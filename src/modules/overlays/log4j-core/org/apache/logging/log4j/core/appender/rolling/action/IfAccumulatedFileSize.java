package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rolling.FileSize;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(printObject = true)
@Plugin
public final class IfAccumulatedFileSize implements PathCondition {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final long thresholdBytes;
   private long accumulatedSize;
   private final PathCondition[] nestedConditions;

   private IfAccumulatedFileSize(final long thresholdSize, final PathCondition... nestedConditions) {
      if (thresholdSize <= 0L) {
         throw new IllegalArgumentException("Count must be a positive integer but was " + thresholdSize);
      }

      this.thresholdBytes = thresholdSize;
      this.nestedConditions = PathCondition.copy(nestedConditions);
   }

   public long getThresholdBytes() {
      return this.thresholdBytes;
   }

   public List<PathCondition> getNestedConditions() {
      return Collections.unmodifiableList(Arrays.asList(this.nestedConditions));
   }

   @Override
   public boolean accept(final Path basePath, final Path relativePath, final BasicFileAttributes attrs) {
      this.accumulatedSize = this.accumulatedSize + attrs.size();
      boolean result = this.accumulatedSize > this.thresholdBytes;
      String match = result ? ">" : "<=";
      String accept = result ? "ACCEPTED" : "REJECTED";
      LOGGER.trace(
         "IfAccumulatedFileSize {}: {} accumulated size '{}' {} thresholdBytes '{}'", accept, relativePath, this.accumulatedSize, match, this.thresholdBytes
      );
      return result ? IfAll.accept(this.nestedConditions, basePath, relativePath, attrs) : result;
   }

   @Override
   public void beforeFileTreeWalk() {
      this.accumulatedSize = 0L;
      IfAll.beforeFileTreeWalk(this.nestedConditions);
   }

   @PluginFactory
   public static IfAccumulatedFileSize createFileSizeCondition(
      @PluginAttribute("exceeds") final String size, @PluginElement("PathConditions") final PathCondition... nestedConditions
   ) {
      if (size == null) {
         LOGGER.error("IfAccumulatedFileSize missing mandatory size threshold.");
      }

      long threshold = size == null ? Long.MAX_VALUE : FileSize.parse(size, Long.MAX_VALUE);
      return new IfAccumulatedFileSize(threshold, nestedConditions);
   }

   @Override
   public String toString() {
      String nested = this.nestedConditions.length == 0 ? "" : " AND " + Arrays.toString(this.nestedConditions);
      return "IfAccumulatedFileSize(exceeds=" + this.thresholdBytes + nested + ")";
   }
}
