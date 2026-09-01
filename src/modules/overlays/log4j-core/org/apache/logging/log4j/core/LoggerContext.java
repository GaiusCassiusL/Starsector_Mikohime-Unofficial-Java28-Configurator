package org.apache.logging.log4j.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.jmx.Server;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ExecutorServices;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.LoggerContextShutdownAware;
import org.apache.logging.log4j.spi.LoggerContextShutdownEnabled;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.apache.logging.log4j.spi.Terminable;
import org.apache.logging.log4j.util.PropertiesUtil;

public class LoggerContext
   extends AbstractLifeCycle
   implements org.apache.logging.log4j.spi.LoggerContext,
   AutoCloseable,
   Terminable,
   ConfigurationListener,
   LoggerContextShutdownEnabled {
   public static final String PROPERTY_CONFIG = "config";
   public static final Key<WeakReference<LoggerContext>> KEY = new Key<WeakReference<LoggerContext>>() {};
   private static final Configuration NULL_CONFIGURATION = new NullConfiguration();
   private final LoggerRegistry<Logger> loggerRegistry = new LoggerRegistry();
   private final CopyOnWriteArrayList<PropertyChangeListener> propertyChangeListeners = new CopyOnWriteArrayList<>();
   private volatile List<LoggerContextShutdownAware> listeners;
   private final Injector injector;
   private PropertiesUtil properties;
   private volatile Configuration configuration = new DefaultConfiguration();
   private static final String EXTERNAL_CONTEXT_KEY = "__EXTERNAL_CONTEXT_KEY__";
   private final ConcurrentMap<String, Object> externalMap = new ConcurrentHashMap<>();
   private String contextName;
   private volatile URI configLocation;
   private Cancellable shutdownCallback;
   private final Lock configLock = new ReentrantLock();

   public LoggerContext(final String name) {
      this(name, null, (URI)null);
   }

   public LoggerContext(final String name, final Object externalContext) {
      this(name, externalContext, (URI)null);
   }

   public LoggerContext(final String name, final Object externalContext, final URI configLocn) {
      this(name, externalContext, configLocn, DI.createInjector());
      this.injector.init();
      this.injector.registerBindingIfAbsent(KEY, () -> new WeakReference<>(this));
   }

   public LoggerContext(final String name, final Object externalContext, final URI configLocn, final Injector injector) {
      this.contextName = name;
      if (externalContext != null) {
         this.externalMap.put("__EXTERNAL_CONTEXT_KEY__", externalContext);
      }

      this.configLocation = configLocn;
      this.injector = injector.copy();
      injector.registerBindingIfAbsent(KEY, () -> new WeakReference<>(this));
   }

   public LoggerContext(final String name, final Object externalContext, final String configLocn) {
      this(name, externalContext, configLocn, DI.createInjector());
      this.injector.init();
      this.injector.registerBindingIfAbsent(KEY, () -> new WeakReference<>(this));
   }

   public LoggerContext(final String name, final Object externalContext, final String configLocn, final Injector injector) {
      this.contextName = name;
      if (externalContext != null) {
         this.externalMap.put("__EXTERNAL_CONTEXT_KEY__", externalContext);
      }

      if (configLocn != null) {
         URI uri;
         try {
            uri = new File(configLocn).toURI();
         } catch (Exception ex) {
            uri = null;
         }

         this.configLocation = uri;
      } else {
         this.configLocation = null;
      }

      this.injector = injector.copy();
      this.injector.registerBindingIfAbsent(KEY, () -> new WeakReference<>(this));
   }

   public void setProperties(final PropertiesUtil properties) {
      this.properties = properties;
   }

   public PropertiesUtil getProperties() {
      return this.properties;
   }

   public void addShutdownListener(final LoggerContextShutdownAware listener) {
      if (this.listeners == null) {
         synchronized (this) {
            if (this.listeners == null) {
               this.listeners = new CopyOnWriteArrayList<>();
            }
         }
      }

      this.listeners.add(listener);
   }

   public List<LoggerContextShutdownAware> getListeners() {
      return this.listeners;
   }

   public static LoggerContext getContext() {
      return (LoggerContext)LogManager.getContext();
   }

   public static LoggerContext getContext(final boolean currentContext) {
      return (LoggerContext)LogManager.getContext(currentContext);
   }

   public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext, final URI configLocation) {
      return (LoggerContext)LogManager.getContext(loader, currentContext, configLocation);
   }

   @Override
   public void start() {
      LOGGER.debug("Starting {}...", this);
      if (this.getProperties().getBooleanProperty(Log4jPropertyKey.STACKTRACE_ON_START, false)) {
         LOGGER.debug("Stack trace to locate invoker", new Exception("Not a real error, showing stack trace to locate invoker"));
      }

      if (this.configLock.tryLock()) {
         try {
            if (this.isInitialized() || this.isStopped()) {
               this.setStarting();
               this.reconfigure();
               if (this.configuration.isShutdownHookEnabled()) {
                  this.setUpShutdownHook();
               }

               this.setStarted();
            }
         } finally {
            this.configLock.unlock();
         }
      }

      LOGGER.debug("{} started OK.", this);
   }

   public void start(final Configuration config) {
      LOGGER.debug("Starting {} with configuration {}...", this, config);
      if (this.configLock.tryLock()) {
         try {
            if (this.isInitialized() || this.isStopped()) {
               if (config.isShutdownHookEnabled()) {
                  this.setUpShutdownHook();
               }

               this.setStarted();
            }
         } finally {
            this.configLock.unlock();
         }
      }

      this.setConfiguration(config);
      LOGGER.debug("{} started OK with configuration {}.", this, config);
   }

   private void setUpShutdownHook() {
      if (this.shutdownCallback == null) {
         LoggerContextFactory factory = LogManager.getFactory();
         if (factory instanceof ShutdownCallbackRegistry) {
            LOGGER.debug(ShutdownCallbackRegistry.SHUTDOWN_HOOK_MARKER, "Shutdown hook enabled. Registering a new one.");
            ExecutorServices.ensureInitialized();

            try {
               final long shutdownTimeoutMillis = this.configuration.getShutdownTimeoutMillis();
               this.shutdownCallback = ((ShutdownCallbackRegistry)factory).addShutdownCallback(new Runnable() {
                  @Override
                  public void run() {
                     LoggerContext context = LoggerContext.this;
                     AbstractLifeCycle.LOGGER.debug(ShutdownCallbackRegistry.SHUTDOWN_HOOK_MARKER, "Stopping {}", context);
                     context.stop(shutdownTimeoutMillis, TimeUnit.MILLISECONDS);
                  }

                  @Override
                  public String toString() {
                     return "Shutdown callback for LoggerContext[name=" + LoggerContext.this.getName() + "]";
                  }
               });
            } catch (IllegalStateException e) {
               throw new IllegalStateException("Unable to register Log4j shutdown hook because JVM is shutting down.", e);
            } catch (SecurityException e) {
               LOGGER.error(ShutdownCallbackRegistry.SHUTDOWN_HOOK_MARKER, "Unable to register shutdown hook due to security restrictions", e);
            }
         }
      }
   }

   @Override
   public void close() {
      this.stop();
   }

   public void terminate() {
      this.stop();
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      LOGGER.debug("Stopping {}...", this);
      this.configLock.lock();

      try {
         if (this.isStopped()) {
            return true;
         }

         this.setStopping();

         try {
            Server.unregisterLoggerContext(this.getName());
         } catch (LinkageError | Exception e) {
            LOGGER.error("Unable to unregister MBeans", e);
         }

         if (this.shutdownCallback != null) {
            this.shutdownCallback.cancel();
            this.shutdownCallback = null;
         }

         Configuration prev = this.configuration;
         this.configuration = NULL_CONFIGURATION;
         this.updateLoggers();
         prev.stop(timeout, timeUnit);
         this.externalMap.clear();
         LogManager.getFactory().removeContext(this);
      } finally {
         this.configLock.unlock();
         this.setStopped();
      }

      if (this.listeners != null) {
         for (LoggerContextShutdownAware listener : this.listeners) {
            try {
               listener.contextShutdown(this);
            } catch (Exception var10) {
            }
         }
      }

      LOGGER.debug("Stopped {} with status {}", this, true);
      return true;
   }

   public String getName() {
      return this.contextName;
   }

   public Logger getRootLogger() {
      return this.getLogger("");
   }

   public void setName(final String name) {
      this.contextName = Objects.requireNonNull(name);
   }

   public Object getObject(final String key) {
      return this.externalMap.get(key);
   }

   public Object putObject(final String key, final Object value) {
      return this.externalMap.put(key, value);
   }

   public Object putObjectIfAbsent(final String key, final Object value) {
      return this.externalMap.putIfAbsent(key, value);
   }

   public Object removeObject(final String key) {
      return this.externalMap.remove(key);
   }

   public boolean removeObject(final String key, final Object value) {
      return this.externalMap.remove(key, value);
   }

   public void setExternalContext(final Object context) {
      if (context != null) {
         this.externalMap.put("__EXTERNAL_CONTEXT_KEY__", context);
      } else {
         this.externalMap.remove("__EXTERNAL_CONTEXT_KEY__");
      }
   }

   public Object getExternalContext() {
      return this.externalMap.get("__EXTERNAL_CONTEXT_KEY__");
   }

   public Logger getLogger(final String name) {
      return this.getLogger(name, null);
   }

   public Collection<Logger> getLoggers() {
      return this.loggerRegistry.getLoggers();
   }

   public Logger getLogger(final String name, final MessageFactory messageFactory) {
      Logger logger = (Logger)this.loggerRegistry.getLogger(name, messageFactory);
      if (logger != null) {
         AbstractLogger.checkMessageFactory(logger, messageFactory);
         return logger;
      } else {
         logger = this.newInstance(this, name, messageFactory);
         this.loggerRegistry.putIfAbsent(name, messageFactory, logger);
         return (Logger)this.loggerRegistry.getLogger(name, messageFactory);
      }
   }

   public LoggerRegistry<Logger> getLoggerRegistry() {
      return this.loggerRegistry;
   }

   public Injector getInjector() {
      return this.injector;
   }

   public boolean hasLogger(final String name) {
      return this.loggerRegistry.hasLogger(name);
   }

   public boolean hasLogger(final String name, final MessageFactory messageFactory) {
      return this.loggerRegistry.hasLogger(name, messageFactory);
   }

   public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
      return this.loggerRegistry.hasLogger(name, messageFactoryClass);
   }

   public Configuration getConfiguration() {
      return this.configuration;
   }

   public void addFilter(final Filter filter) {
      this.configuration.addFilter(filter);
   }

   public void removeFilter(final Filter filter) {
      this.configuration.removeFilter(filter);
   }

   public Configuration setConfiguration(final Configuration config) {
      if (config == null) {
         LOGGER.error("No configuration found for context '{}'.", this.contextName);
         return this.configuration;
      }

      this.configLock.lock();

      try {
         Configuration prev = this.configuration;
         config.addListener(this);
         ConcurrentMap<String, String> map = config.getComponent("ContextProperties");

         try {
            map.computeIfAbsent("hostName", s -> NetUtils.getLocalHostname());
         } catch (Exception ex) {
            LOGGER.debug("Ignoring {}, setting hostName to 'unknown'", ex.toString());
            map.putIfAbsent("hostName", "unknown");
         }

         map.putIfAbsent("contextName", this.contextName);
         config.start();
         this.configuration = config;
         this.updateLoggers();
         if (prev != null) {
            prev.removeListener(this);
            prev.stop();
         }

         this.firePropertyChangeEvent(new PropertyChangeEvent(this, "config", prev, config));

         try {
            Server.reregisterMBeansAfterReconfigure();
         } catch (LinkageError | Exception e) {
            LOGGER.error("Could not reconfigure JMX", e);
         }

         return prev;
      } finally {
         this.configLock.unlock();
      }
   }

   private void firePropertyChangeEvent(final PropertyChangeEvent event) {
      for (PropertyChangeListener listener : this.propertyChangeListeners) {
         listener.propertyChange(event);
      }
   }

   public void addPropertyChangeListener(final PropertyChangeListener listener) {
      this.propertyChangeListeners.add(Objects.requireNonNull(listener, "listener"));
   }

   public void removePropertyChangeListener(final PropertyChangeListener listener) {
      this.propertyChangeListeners.remove(listener);
   }

   public URI getConfigLocation() {
      return this.configLocation;
   }

   public void setConfigLocation(final URI configLocation) {
      this.configLocation = configLocation;
      this.reconfigure(configLocation);
   }

   private void reconfigure(final URI configURI) {
      Object externalContext = this.externalMap.get("__EXTERNAL_CONTEXT_KEY__");
      ClassLoader cl = externalContext instanceof ClassLoader ? (ClassLoader)externalContext : null;
      LOGGER.debug("Reconfiguration started for {} at URI {} with optional ClassLoader: {}", this, configURI, cl);
      boolean setProperties = false;
      if (this.properties != null && !PropertiesUtil.hasThreadProperties()) {
         PropertiesUtil.setThreadProperties(this.properties);
      }

      try {
         Configuration instance = ((ConfigurationFactory)this.injector.getInstance(ConfigurationFactory.KEY))
            .getConfiguration(this, this.contextName, configURI, cl);
         if (instance == null) {
            LOGGER.error("Reconfiguration failed: No configuration found for '{}' at '{}' in '{}'", this.contextName, configURI, cl);
         } else {
            this.setConfiguration(instance);
            String location = this.configuration == null ? "?" : String.valueOf(this.configuration.getConfigurationSource());
            LOGGER.debug("Reconfiguration complete for {} at URI {} with optional ClassLoader: {}", this, location, cl);
         }
      } finally {
         ;
      }
   }

   public void reconfigure() {
      this.reconfigure(this.configLocation);
   }

   public void reconfigure(final Configuration configuration) {
      this.setConfiguration(configuration);
      ConfigurationSource source = configuration.getConfigurationSource();
      if (source != null) {
         URI uri = source.getURI();
         if (uri != null) {
            this.configLocation = uri;
         }
      }
   }

   public void updateLoggers() {
      this.updateLoggers(this.configuration);
   }

   public void updateLoggers(final Configuration config) {
      Configuration old = this.configuration;

      for (Logger logger : this.loggerRegistry.getLoggers()) {
         logger.updateConfiguration(config);
      }

      this.firePropertyChangeEvent(new PropertyChangeEvent(this, "config", old, config));
   }

   @Override
   public synchronized void onChange(final Reconfigurable reconfigurable) {
      long startMillis = System.currentTimeMillis();
      LOGGER.debug("Reconfiguration started for context {} ({})", this.contextName, this);
      this.initApiModule();
      Configuration newConfig = reconfigurable.reconfigure();
      if (newConfig != null) {
         this.setConfiguration(newConfig);
         LOGGER.debug("Reconfiguration completed for {} ({}) in {} milliseconds.", this.contextName, this, System.currentTimeMillis() - startMillis);
      } else {
         LOGGER.debug("Reconfiguration failed for {} ({}) in {} milliseconds.", this.contextName, this, System.currentTimeMillis() - startMillis);
      }
   }

   @Override
   public String toString() {
      return "LoggerContext[" + this.contextName + "]";
   }

   private void initApiModule() {
      ThreadContext.init();
   }

   protected Logger newInstance(final LoggerContext ctx, final String name, final MessageFactory messageFactory) {
      return new Logger(ctx, name, messageFactory);
   }
}
