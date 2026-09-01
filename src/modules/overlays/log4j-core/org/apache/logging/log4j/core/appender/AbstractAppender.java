package org.apache.logging.log4j.core.appender;

import java.nio.charset.Charset;
import java.util.Objects;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

public abstract class AbstractAppender extends AbstractFilterable implements Appender {
   private final String name;
   private final boolean ignoreExceptions;
   private final Layout layout;
   private ErrorHandler handler = new DefaultErrorHandler(this);

   protected AbstractAppender(final String name, final Filter filter, final Layout layout, final boolean ignoreExceptions, final Property[] properties) {
      super(filter, properties);
      this.name = Objects.requireNonNull(name, "name");
      this.layout = layout;
      this.ignoreExceptions = ignoreExceptions;
   }

   @Deprecated
   protected AbstractAppender(final String name, final Filter filter, final Layout layout) {
      this(name, filter, layout, true, Property.EMPTY_ARRAY);
   }

   @Deprecated
   protected AbstractAppender(final String name, final Filter filter, final Layout layout, final boolean ignoreExceptions) {
      this(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
   }

   public static int parseInt(final String s, final int defaultValue) {
      try {
         return Integers.parseInt(s, defaultValue);
      } catch (NumberFormatException e) {
         LOGGER.error("Could not parse \"{}\" as an integer,  using default value {}: {}", s, defaultValue, e);
         return defaultValue;
      }
   }

   @Override
   public boolean requiresLocation() {
      return this.layout != null && this.layout.requiresLocation();
   }

   public void error(final String msg) {
      this.handler.error(msg);
   }

   public void error(final String msg, final LogEvent event, final Throwable t) {
      this.handler.error(msg, event, t);
   }

   public void error(final String msg, final Throwable t) {
      this.handler.error(msg, t);
   }

   @Override
   public ErrorHandler getHandler() {
      return this.handler;
   }

   @Override
   public Layout getLayout() {
      return this.layout;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public boolean ignoreExceptions() {
      return this.ignoreExceptions;
   }

   @Override
   public void setHandler(final ErrorHandler handler) {
      if (handler == null) {
         LOGGER.error("The handler cannot be set to null");
      } else if (this.isStarted()) {
         LOGGER.error("The handler cannot be changed once the appender is started");
      } else {
         this.handler = handler;
      }
   }

   @Override
   public String toString() {
      return this.name;
   }

   public abstract static class Builder<B extends AbstractAppender.Builder<B>> extends AbstractFilterable.Builder<B> {
      @PluginBuilderAttribute
      private boolean ignoreExceptions = true;
      @PluginElement("Layout")
      private Layout layout;
      @PluginBuilderAttribute
      @Required(message = "No appender name provided")
      private String name;
      @PluginConfiguration
      private Configuration configuration;

      public String getName() {
         return this.name;
      }

      public boolean isIgnoreExceptions() {
         return this.ignoreExceptions;
      }

      public Layout getLayout() {
         return this.layout;
      }

      public B setName(final String name) {
         this.name = name;
         return this.asBuilder();
      }

      public B setIgnoreExceptions(final boolean ignoreExceptions) {
         this.ignoreExceptions = ignoreExceptions;
         return this.asBuilder();
      }

      public B setLayout(final Layout layout) {
         this.layout = layout;
         return this.asBuilder();
      }

      public Layout getOrCreateLayout() {
         return this.layout == null ? PatternLayout.createDefaultLayout() : this.layout;
      }

      public Layout getOrCreateLayout(final Charset charset) {
         return this.layout == null ? PatternLayout.newBuilder().setCharset(charset).build() : this.layout;
      }

      public B setConfiguration(final Configuration configuration) {
         this.configuration = configuration;
         return this.asBuilder();
      }

      public Configuration getConfiguration() {
         return this.configuration;
      }
   }
}
