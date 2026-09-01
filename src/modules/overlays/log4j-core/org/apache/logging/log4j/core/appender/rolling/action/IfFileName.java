package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
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
public final class IfFileName implements PathCondition {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final PathMatcher pathMatcher;
   private final String syntaxAndPattern;
   private final PathCondition[] nestedConditions;

   private IfFileName(final String glob, final String regex, final PathCondition... nestedConditions) {
      if (regex == null && glob == null) {
         throw new IllegalArgumentException("Specify either a path glob or a regular expression. Both cannot be null.");
      }

      this.syntaxAndPattern = createSyntaxAndPatternString(glob, regex);
      this.pathMatcher = FileSystems.getDefault().getPathMatcher(this.syntaxAndPattern);
      this.nestedConditions = PathCondition.copy(nestedConditions);
   }

   static String createSyntaxAndPatternString(final String glob, final String regex) {
      if (glob != null) {
         return glob.startsWith("glob:") ? glob : "glob:" + glob;
      } else {
         return regex.startsWith("regex:") ? regex : "regex:" + regex;
      }
   }

   public String getSyntaxAndPattern() {
      return this.syntaxAndPattern;
   }

   public List<PathCondition> getNestedConditions() {
      return Collections.unmodifiableList(Arrays.asList(this.nestedConditions));
   }

   @Override
   public boolean accept(final Path basePath, final Path relativePath, final BasicFileAttributes attrs) {
      boolean result = this.pathMatcher.matches(relativePath);
      String match = result ? "matches" : "does not match";
      String accept = result ? "ACCEPTED" : "REJECTED";
      LOGGER.trace("IfFileName {}: '{}' {} relative path '{}'", accept, this.syntaxAndPattern, match, relativePath);
      return result ? IfAll.accept(this.nestedConditions, basePath, relativePath, attrs) : result;
   }

   @Override
   public void beforeFileTreeWalk() {
      IfAll.beforeFileTreeWalk(this.nestedConditions);
   }

   @PluginFactory
   public static IfFileName createNameCondition(
      @PluginAttribute final String glob, @PluginAttribute final String regex, @PluginElement("PathConditions") final PathCondition... nestedConditions
   ) {
      return new IfFileName(glob, regex, nestedConditions);
   }

   @Override
   public String toString() {
      String nested = this.nestedConditions.length == 0 ? "" : " AND " + Arrays.toString(this.nestedConditions);
      return "IfFileName(" + this.syntaxAndPattern + nested + ")";
   }
}
