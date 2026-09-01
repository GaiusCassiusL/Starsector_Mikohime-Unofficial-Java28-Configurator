package org.apache.logging.log4j.core.async;

import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(printObject = true)
@Plugin("AsyncWaitStrategyFactory")
public class AsyncWaitStrategyFactoryConfig {
   protected static final Logger LOGGER = StatusLogger.getLogger();
   private final String factoryClassName;

   public AsyncWaitStrategyFactoryConfig(final String factoryClassName) {
      this.factoryClassName = Objects.requireNonNull(factoryClassName, "factoryClassName");
   }

   @PluginFactory
   public static <B extends AsyncWaitStrategyFactoryConfig.Builder<B>> B newBuilder() {
      return new AsyncWaitStrategyFactoryConfig.Builder<B>().asBuilder();
   }

   public AsyncWaitStrategyFactory createWaitStrategyFactory() {
      try {
         Class<? extends AsyncWaitStrategyFactory> klass = (Class<? extends AsyncWaitStrategyFactory>)Loader.loadClass(this.factoryClassName);
         if (AsyncWaitStrategyFactory.class.isAssignableFrom(klass)) {
            return klass.newInstance();
         }

         LOGGER.error("Ignoring factory '{}': it is not assignable to AsyncWaitStrategyFactory", this.factoryClassName);
         return null;
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
         LOGGER.info("Invalid implementation class name value: error creating AsyncWaitStrategyFactory {}: {}", this.factoryClassName, e);
         return null;
      }
   }

   public static class Builder<B extends AsyncWaitStrategyFactoryConfig.Builder<B>>
      implements org.apache.logging.log4j.core.util.Builder<AsyncWaitStrategyFactoryConfig> {
      @PluginBuilderAttribute("class")
      @Required(message = "AsyncWaitStrategyFactory cannot be configured without a factory class name")
      private String factoryClassName;

      public String getFactoryClassName() {
         return this.factoryClassName;
      }

      public B withFactoryClassName(final String className) {
         this.factoryClassName = className;
         return this.asBuilder();
      }

      public AsyncWaitStrategyFactoryConfig build() {
         return new AsyncWaitStrategyFactoryConfig(this.factoryClassName);
      }

      public B asBuilder() {
         return (B)this;
      }
   }
}
