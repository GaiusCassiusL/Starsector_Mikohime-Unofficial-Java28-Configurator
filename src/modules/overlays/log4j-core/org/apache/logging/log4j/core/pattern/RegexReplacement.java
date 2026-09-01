package org.apache.logging.log4j.core.pattern;

import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(printObject = true)
@Plugin("replace")
public final class RegexReplacement {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final Pattern pattern;
   private final String substitution;

   private RegexReplacement(final Pattern pattern, final String substitution) {
      this.pattern = pattern;
      this.substitution = substitution;
   }

   public String format(final String msg) {
      return this.pattern.matcher(msg).replaceAll(this.substitution);
   }

   @Override
   public String toString() {
      return "replace(regex=" + this.pattern.pattern() + ", replacement=" + this.substitution + ")";
   }

   @PluginFactory
   public static RegexReplacement createRegexReplacement(@PluginAttribute final Pattern regex, @PluginAttribute final String replacement) {
      if (regex == null) {
         LOGGER.error("A regular expression is required for replacement");
         return null;
      }

      if (replacement == null) {
         LOGGER.error("A replacement string is required to perform replacement");
      }

      return new RegexReplacement(regex, replacement);
   }
}
