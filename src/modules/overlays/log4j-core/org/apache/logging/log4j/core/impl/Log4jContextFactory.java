package org.apache.logging.log4j.core.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.StackLocatorUtil;

@Singleton
public class Log4jContextFactory implements LoggerContextFactory, ShutdownCallbackRegistry {
   private static final StatusLogger LOGGER = StatusLogger.getLogger();
   private final Injector injector;
   private final ContextSelector selector;
   private final ShutdownCallbackRegistry shutdownCallbackRegistry;

   public Log4jContextFactory() {
      this.injector = DI.createInjector();
      this.injector.init();
      this.selector = (ContextSelector)this.injector.getInstance(ContextSelector.KEY);
      this.shutdownCallbackRegistry = (ShutdownCallbackRegistry)this.injector.getInstance(ShutdownCallbackRegistry.KEY);
      LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
      this.initializeShutdownCallbackRegistry();
   }

   public Log4jContextFactory(final ContextSelector selector) {
      Objects.requireNonNull(selector, "No ContextSelector provided");
      this.injector = DI.createInjector();
      this.injector.init();
      this.injector.registerBinding(ContextSelector.KEY, () -> selector);
      this.selector = (ContextSelector)this.injector.getInstance(ContextSelector.KEY);
      this.shutdownCallbackRegistry = (ShutdownCallbackRegistry)this.injector.getInstance(ShutdownCallbackRegistry.KEY);
      LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
      this.initializeShutdownCallbackRegistry();
   }

   public Log4jContextFactory(final ShutdownCallbackRegistry shutdownCallbackRegistry) {
      Objects.requireNonNull(shutdownCallbackRegistry, "No ShutdownCallbackRegistry provided");
      this.injector = DI.createInjector();
      this.injector.init();
      this.injector.registerBinding(ShutdownCallbackRegistry.KEY, () -> shutdownCallbackRegistry);
      this.selector = (ContextSelector)this.injector.getInstance(ContextSelector.KEY);
      this.shutdownCallbackRegistry = (ShutdownCallbackRegistry)this.injector.getInstance(ShutdownCallbackRegistry.KEY);
      LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
      this.initializeShutdownCallbackRegistry();
   }

   public Log4jContextFactory(final ContextSelector selector, final ShutdownCallbackRegistry shutdownCallbackRegistry) {
      Objects.requireNonNull(selector, "No ContextSelector provided");
      Objects.requireNonNull(shutdownCallbackRegistry, "No ShutdownCallbackRegistry provided");
      this.injector = DI.createInjector();
      this.injector.init();
      this.injector.registerBinding(ContextSelector.KEY, () -> selector).registerBinding(ShutdownCallbackRegistry.KEY, () -> shutdownCallbackRegistry);
      this.selector = (ContextSelector)this.injector.getInstance(ContextSelector.KEY);
      this.shutdownCallbackRegistry = (ShutdownCallbackRegistry)this.injector.getInstance(ShutdownCallbackRegistry.KEY);
      LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
      this.initializeShutdownCallbackRegistry();
   }

   @Inject
   public Log4jContextFactory(final Injector injector, final ContextSelector selector, final ShutdownCallbackRegistry registry) {
      this.injector = injector;
      this.selector = selector;
      this.shutdownCallbackRegistry = registry;
      LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
      this.initializeShutdownCallbackRegistry();
   }

   public Log4jContextFactory(final Injector injector) {
      this(injector, (ContextSelector)injector.getInstance(ContextSelector.KEY), (ShutdownCallbackRegistry)injector.getInstance(ShutdownCallbackRegistry.KEY));
   }

   private void initializeShutdownCallbackRegistry() {
      if (this.isShutdownHookEnabled() && this.shutdownCallbackRegistry instanceof LifeCycle) {
         try {
            ((LifeCycle)this.shutdownCallbackRegistry).start();
         } catch (IllegalStateException e) {
            LOGGER.error("Cannot start ShutdownCallbackRegistry, already shutting down.");
            throw e;
         } catch (RuntimeException e) {
            LOGGER.error("There was an error starting the ShutdownCallbackRegistry.", e);
         }
      }
   }

   public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext, final boolean currentContext) {
      ClassLoader classLoader = loader != null ? loader : StackLocatorUtil.getCallerClass(fqcn).getClassLoader();
      LoggerContext ctx = this.selector.getContext(fqcn, classLoader, currentContext);
      if (externalContext != null && ctx.getExternalContext() == null) {
         ctx.setExternalContext(externalContext);
      }

      if (ctx.getState() == LifeCycle.State.INITIALIZED) {
         this.startContext(ctx, classLoader);
      }

      return ctx;
   }

   public LoggerContext getContext(
      final String fqcn, final ClassLoader loader, final Object externalContext, final boolean currentContext, final ConfigurationSource source
   ) {
      ClassLoader classLoader = loader != null ? loader : StackLocatorUtil.getCallerClass(fqcn).getClassLoader();
      LoggerContext ctx = this.selector.getContext(fqcn, classLoader, currentContext, null);
      if (externalContext != null && ctx.getExternalContext() == null) {
         ctx.setExternalContext(externalContext);
      }

      if (ctx.getState() != LifeCycle.State.INITIALIZED) {
         return ctx;
      }

      if (source != null) {
         ContextAnchor.THREAD_CONTEXT.set(ctx);
         boolean setProperties = false;

         try {
            if (ctx.getProperties() == null) {
               PropertiesUtil props = PropertiesUtil.getContextProperties(classLoader, ctx.getName());
               ctx.setProperties(props);
               PropertiesUtil.setThreadProperties(props);
               setProperties = true;
            }

            Configuration config = ((ConfigurationFactory)this.injector.getInstance(ConfigurationFactory.KEY)).getConfiguration(ctx, source);
            LOGGER.debug("Starting {} from configuration {}", ctx, source);
            ctx.start(config);
         } finally {
            if (setProperties) {
               PropertiesUtil.clearThreadProperties();
            }

            ContextAnchor.THREAD_CONTEXT.remove();
         }
      } else {
         this.startContext(ctx, classLoader);
      }

      return ctx;
   }

   public LoggerContext getContext(
      final String fqcn, final ClassLoader loader, final Object externalContext, final boolean currentContext, final Configuration configuration
   ) {
      ClassLoader classLoader = loader != null ? loader : StackLocatorUtil.getCallerClass(fqcn).getClassLoader();
      LoggerContext ctx = this.selector.getContext(fqcn, classLoader, currentContext, null);
      if (externalContext != null && ctx.getExternalContext() == null) {
         ctx.setExternalContext(externalContext);
      }

      if (ctx.getState() == LifeCycle.State.INITIALIZED) {
         ContextAnchor.THREAD_CONTEXT.set(ctx);
         boolean setProperties = false;

         try {
            if (ctx.getProperties() == null) {
               PropertiesUtil props = PropertiesUtil.getContextProperties(classLoader, ctx.getName());
               ctx.setProperties(props);
               PropertiesUtil.setThreadProperties(props);
               setProperties = true;
            }

            ctx.start(configuration);
         } finally {
            if (setProperties) {
               PropertiesUtil.clearThreadProperties();
            }

            ContextAnchor.THREAD_CONTEXT.remove();
         }
      }

      return ctx;
   }

   public LoggerContext getContext(
      final String fqcn, final ClassLoader loader, final Object externalContext, final boolean currentContext, final URI configLocation, final String name
   ) {
      ClassLoader classLoader = loader != null ? loader : StackLocatorUtil.getCallerClass(fqcn).getClassLoader();
      LoggerContext ctx = this.selector.getContext(fqcn, classLoader, currentContext, configLocation);
      if (externalContext != null && ctx.getExternalContext() == null) {
         ctx.setExternalContext(externalContext);
      }

      if (name != null) {
         ctx.setName(name);
      }

      if (ctx.getState() == LifeCycle.State.INITIALIZED) {
         if (configLocation == null && name == null) {
            this.startContext(ctx, classLoader);
         } else {
            ContextAnchor.THREAD_CONTEXT.set(ctx);
            boolean setProperties = false;

            try {
               if (ctx.getProperties() == null) {
                  PropertiesUtil props = PropertiesUtil.getContextProperties(classLoader, ctx.getName());
                  ctx.setProperties(props);
                  PropertiesUtil.setThreadProperties(props);
                  setProperties = true;
               }

               Configuration config = ((ConfigurationFactory)this.injector.getInstance(ConfigurationFactory.KEY)).getConfiguration(ctx, name, configLocation);
               LOGGER.debug("Starting {} from configuration at {}", ctx, configLocation);
               ctx.start(config);
            } finally {
               if (setProperties) {
                  PropertiesUtil.clearThreadProperties();
               }

               ContextAnchor.THREAD_CONTEXT.remove();
            }
         }
      }

      return ctx;
   }

   public LoggerContext getContext(
      final String fqcn, final ClassLoader loader, final Entry<String, Object> entry, final boolean currentContext, final URI configLocation, final String name
   ) {
      ClassLoader classLoader = loader != null ? loader : StackLocatorUtil.getCallerClass(fqcn).getClassLoader();
      LoggerContext ctx = this.selector.getContext(fqcn, classLoader, entry, currentContext, configLocation);
      if (name != null) {
         ctx.setName(name);
      }

      if (ctx.getState() == LifeCycle.State.INITIALIZED) {
         if (configLocation == null && name == null) {
            this.startContext(ctx, classLoader);
         } else {
            boolean setProperties = false;

            try {
               if (ctx.getProperties() == null) {
                  PropertiesUtil props = PropertiesUtil.getContextProperties(classLoader, ctx.getName());
                  ctx.setProperties(props);
                  PropertiesUtil.setThreadProperties(props);
                  setProperties = true;
               }

               ContextAnchor.THREAD_CONTEXT.set(ctx);
               Configuration config = ((ConfigurationFactory)this.injector.getInstance(ConfigurationFactory.KEY)).getConfiguration(ctx, name, configLocation);
               LOGGER.debug("Starting {} from configuration at {}", ctx, configLocation);
               ctx.start(config);
            } finally {
               if (setProperties) {
                  PropertiesUtil.clearThreadProperties();
               }

               ContextAnchor.THREAD_CONTEXT.remove();
            }
         }
      }

      return ctx;
   }

   public LoggerContext getContext(
      final String fqcn,
      final ClassLoader loader,
      final Object externalContext,
      final boolean currentContext,
      final List<URI> configLocations,
      final String name
   ) {
      ClassLoader classLoader = loader != null ? loader : StackLocatorUtil.getCallerClass(fqcn).getClassLoader();
      LoggerContext ctx = this.selector.getContext(fqcn, classLoader, currentContext, null);
      if (externalContext != null && ctx.getExternalContext() == null) {
         ctx.setExternalContext(externalContext);
      }

      if (name != null) {
         ctx.setName(name);
      }

      if (ctx.getState() != LifeCycle.State.INITIALIZED) {
         return ctx;
      }

      if (configLocations != null && !configLocations.isEmpty()) {
         ContextAnchor.THREAD_CONTEXT.set(ctx);
         boolean setProperties = false;

         try {
            List<AbstractConfiguration> configurations = new ArrayList<>(configLocations.size());
            if (ctx.getProperties() == null) {
               PropertiesUtil props = PropertiesUtil.getContextProperties(classLoader, ctx.getName());
               ctx.setProperties(props);
               PropertiesUtil.setThreadProperties(props);
               setProperties = true;
            }

            for (URI configLocation : configLocations) {
               Configuration currentReadConfiguration = ((ConfigurationFactory)this.injector.getInstance(ConfigurationFactory.KEY))
                  .getConfiguration(ctx, name, configLocation);
               if (currentReadConfiguration != null) {
                  if (currentReadConfiguration instanceof DefaultConfiguration) {
                     LOGGER.warn("Unable to locate configuration {}, ignoring", configLocation.toString());
                  } else if (currentReadConfiguration instanceof AbstractConfiguration) {
                     configurations.add((AbstractConfiguration)currentReadConfiguration);
                  } else {
                     LOGGER.error(
                        "Found configuration {}, which is not an AbstractConfiguration and can't be handled by CompositeConfiguration", configLocation
                     );
                  }
               } else {
                  LOGGER.info("Unable to access configuration {}, ignoring", configLocation.toString());
               }
            }

            if (configurations.size() == 0) {
               LOGGER.error("No configurations could be created for {}", configLocations.toString());
               return ctx;
            }

            if (configurations.size() == 1) {
               ctx.start(configurations.get(0));
            } else {
               CompositeConfiguration compositeConfiguration = new CompositeConfiguration(configurations);
               ctx.start(compositeConfiguration);
            }

            return ctx;
         } finally {
            if (setProperties) {
               PropertiesUtil.clearThreadProperties();
            }

            ContextAnchor.THREAD_CONTEXT.remove();
         }
      } else {
         this.startContext(ctx, classLoader);
         return ctx;
      }
   }

   private void startContext(final LoggerContext ctx, final ClassLoader classLoader) {
      boolean setProperties = false;

      try {
         if (ctx.getProperties() == null) {
            PropertiesUtil props = PropertiesUtil.getContextProperties(classLoader, ctx.getName());
            ctx.setProperties(props);
            PropertiesUtil.setThreadProperties(props);
            setProperties = true;
         }

         Configuration config = ((ConfigurationFactory)this.injector.getInstance(ConfigurationFactory.KEY)).getConfiguration(ctx, ctx.getName(), null);
         if (config != null) {
            ctx.start(config);
         } else {
            ctx.start();
         }
      } finally {
         if (setProperties) {
            PropertiesUtil.clearThreadProperties();
         }
      }
   }

   public void shutdown(final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
      if (this.selector.hasContext(fqcn, loader, currentContext)) {
         this.selector.shutdown(fqcn, loader, currentContext, allContexts);
      }
   }

   public boolean hasContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
      return this.selector.hasContext(fqcn, loader, currentContext);
   }

   public ContextSelector getSelector() {
      return this.selector;
   }

   public ShutdownCallbackRegistry getShutdownCallbackRegistry() {
      return this.shutdownCallbackRegistry;
   }

   public void removeContext(final org.apache.logging.log4j.spi.LoggerContext context) {
      if (context instanceof LoggerContext) {
         this.selector.removeContext((LoggerContext)context);
      }
   }

   public boolean isClassLoaderDependent() {
      return this.selector.isClassLoaderDependent();
   }

   @Override
   public Cancellable addShutdownCallback(final Runnable callback) {
      return this.isShutdownHookEnabled() ? this.shutdownCallbackRegistry.addShutdownCallback(callback) : null;
   }

   public boolean isShutdownHookEnabled() {
      return !Constants.isWebApp() && PropertiesUtil.getProperties().getBooleanProperty(Log4jPropertyKey.SHUTDOWN_HOOK_ENABLED, true);
   }
}
