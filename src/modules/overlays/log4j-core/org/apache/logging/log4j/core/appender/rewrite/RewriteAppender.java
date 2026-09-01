package org.apache.logging.log4j.core.appender.rewrite;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

@Configurable(elementType = "appender", printObject = true)
@Plugin("Rewrite")
public final class RewriteAppender extends AbstractAppender {
   private final Configuration config;
   private final ConcurrentMap<String, AppenderControl> appenders = new ConcurrentHashMap<>();
   private final RewritePolicy rewritePolicy;
   private final AppenderRef[] appenderRefs;

   private RewriteAppender(
      final String name,
      final Filter filter,
      final boolean ignoreExceptions,
      final AppenderRef[] appenderRefs,
      final RewritePolicy rewritePolicy,
      final Configuration config,
      final Property[] properties
   ) {
      super(name, filter, null, ignoreExceptions, properties);
      this.config = config;
      this.rewritePolicy = rewritePolicy;
      this.appenderRefs = appenderRefs;
   }

   @Override
   public void start() {
      for (AppenderRef ref : this.appenderRefs) {
         String name = ref.getRef();
         Appender appender = this.config.getAppender(name);
         if (appender != null) {
            Filter filter = appender instanceof AbstractAppender ? ((AbstractAppender)appender).getFilter() : null;
            this.appenders.put(name, new AppenderControl(appender, ref.getLevel(), filter));
         } else {
            LOGGER.error("Appender " + ref + " cannot be located. Reference ignored");
         }
      }

      super.start();
   }

   @Override
   public void append(LogEvent event) {
      if (this.rewritePolicy != null) {
         event = this.rewritePolicy.rewrite(event);
      }

      for (AppenderControl control : this.appenders.values()) {
         control.callAppender(event);
      }
   }

   @PluginFactory
   public static RewriteAppender createAppender(
      @PluginAttribute @Required(message = "No name provided for RewriteAppender") final String name,
      @PluginAttribute(defaultBoolean = true) final boolean ignoreExceptions,
      @PluginElement @Required(message = "No appender references defined for RewriteAppender") final AppenderRef[] appenderRefs,
      @PluginConfiguration final Configuration config,
      @PluginElement final RewritePolicy rewritePolicy,
      @PluginElement final Filter filter
   ) {
      return new RewriteAppender(name, filter, ignoreExceptions, appenderRefs, rewritePolicy, config, Property.EMPTY_ARRAY);
   }

   @Override
   public boolean requiresLocation() {
      for (AppenderControl control : this.appenders.values()) {
         Appender appender = control.getAppender();
         if (appender.requiresLocation()) {
            return true;
         }
      }

      return false;
   }
}
