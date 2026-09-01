package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "appender", printObject = true)
@Plugin("Null")
public final class NullAppender extends AbstractAppender {
   public static final String PLUGIN_NAME = "Null";

   @PluginFactory
   public static NullAppender createAppender(@PluginAttribute(defaultString = "null") final String name) {
      return new NullAppender(name);
   }

   private NullAppender(final String name) {
      super(name, null, null, true, Property.EMPTY_ARRAY);
   }

   @Override
   public void append(final LogEvent event) {
   }
}
