package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

@Configurable(printObject = true)
@Plugin
@PluginAliases("appender-ref")
public final class AppenderRef {
   private final String ref;
   private final Level level;
   private final Filter filter;

   private AppenderRef(final String ref, final Level level, final Filter filter) {
      this.ref = ref;
      this.level = level;
      this.filter = filter;
   }

   public String getRef() {
      return this.ref;
   }

   public Level getLevel() {
      return this.level;
   }

   public Filter getFilter() {
      return this.filter;
   }

   @Override
   public String toString() {
      return this.ref;
   }

   @PluginFactory
   public static AppenderRef createAppenderRef(
      @PluginAttribute @Required(message = "Appender references must contain a reference") final String ref,
      @PluginAttribute final Level level,
      @PluginElement final Filter filter
   ) {
      return new AppenderRef(ref, level, filter);
   }
}
