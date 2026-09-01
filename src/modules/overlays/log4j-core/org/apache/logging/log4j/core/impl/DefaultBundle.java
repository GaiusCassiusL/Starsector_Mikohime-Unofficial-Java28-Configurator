package org.apache.logging.log4j.core.impl;

import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.annotation.ConditionalOnPropertyKey;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfigurationFactory;
import org.apache.logging.log4j.core.config.composite.DefaultMergeStrategy;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.time.PreciseClock;
import org.apache.logging.log4j.core.time.internal.CachedClock;
import org.apache.logging.log4j.core.time.internal.CoarseCachedClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.core.time.internal.SystemMillisClock;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.di.InjectException;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.spi.CopyOnWrite;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.PropertyKey;

public class DefaultBundle {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final Injector injector;
   private final PropertyEnvironment properties;
   private final ClassLoader classLoader;

   public DefaultBundle(final Injector injector, final PropertyEnvironment properties, final ClassLoader classLoader) {
      this.injector = injector;
      this.properties = properties;
      this.classLoader = classLoader;
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONTEXT_SELECTOR_CLASS_NAME)
   @SingletonFactory
   @Ordered(100)
   public ContextSelector systemPropertyContextSelector() throws ClassNotFoundException {
      return this.newInstanceOfProperty(Log4jPropertyKey.CONTEXT_SELECTOR_CLASS_NAME, ContextSelector.class);
   }

   @SingletonFactory
   public ContextSelector defaultContextSelector() {
      return new ClassLoaderContextSelector(this.injector);
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.SHUTDOWN_CALLBACK_REGISTRY)
   @SingletonFactory
   @Ordered(100)
   public ShutdownCallbackRegistry systemPropertyShutdownCallbackRegistry() throws ClassNotFoundException {
      return this.newInstanceOfProperty(Log4jPropertyKey.SHUTDOWN_CALLBACK_REGISTRY, ShutdownCallbackRegistry.class);
   }

   @SingletonFactory
   public ShutdownCallbackRegistry defaultShutdownCallbackRegistry() {
      return new DefaultShutdownCallbackRegistry();
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONFIG_CLOCK, value = "SystemClock")
   @SingletonFactory
   @Ordered(200)
   public Clock systemClock() {
      return logSupportedPrecision(new SystemClock());
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONFIG_CLOCK, value = "SystemMillisClock")
   @SingletonFactory
   @Ordered(200)
   public Clock systemMillisClock() {
      return logSupportedPrecision(new SystemMillisClock());
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONFIG_CLOCK, value = "CachedClock")
   @SingletonFactory
   @Ordered(200)
   public Clock cachedClock() {
      return logSupportedPrecision(CachedClock.instance());
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONFIG_CLOCK, value = "org.apache.logging.log4j.core.time.internal.CachedClock")
   @SingletonFactory
   @Ordered(200)
   public Clock cachedClockFqcn() {
      return logSupportedPrecision(CachedClock.instance());
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONFIG_CLOCK, value = "CoarseCachedClock")
   @SingletonFactory
   @Ordered(200)
   public Clock coarseCachedClock() {
      return logSupportedPrecision(CoarseCachedClock.instance());
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONFIG_CLOCK, value = "org.apache.logging.log4j.core.time.internal.CoarseCachedClock")
   @SingletonFactory
   @Ordered(200)
   public Clock coarseCachedClockFqcn() {
      return logSupportedPrecision(CoarseCachedClock.instance());
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONFIG_CLOCK)
   @SingletonFactory
   @Ordered(100)
   public Clock systemPropertyClock() throws ClassNotFoundException {
      return logSupportedPrecision(this.newInstanceOfProperty(Log4jPropertyKey.CONFIG_CLOCK, Clock.class));
   }

   @SingletonFactory
   public Clock defaultClock() {
      return new SystemClock();
   }

   @SingletonFactory
   public NanoClock defaultNanoClock() {
      return new DummyNanoClock();
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME)
   @Factory
   @Ordered(100)
   public ContextDataInjector systemPropertyContextDataInjector() throws ClassNotFoundException {
      return this.newInstanceOfProperty(Log4jPropertyKey.THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME, ContextDataInjector.class);
   }

   @Factory
   public ContextDataInjector defaultContextDataInjector() {
      ReadOnlyThreadContextMap threadContextMap = ThreadContext.getThreadContextMap();
      if (threadContextMap instanceof DefaultThreadContextMap || threadContextMap == null) {
         return new ThreadContextDataInjector.ForDefaultThreadContextMap();
      } else {
         return threadContextMap instanceof CopyOnWrite
            ? new ThreadContextDataInjector.ForCopyOnWriteThreadContextMap()
            : new ThreadContextDataInjector.ForGarbageFreeThreadContextMap();
      }
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.LOG_EVENT_FACTORY_CLASS_NAME)
   @SingletonFactory
   @Ordered(100)
   public LogEventFactory systemPropertyLogEventFactory() throws ClassNotFoundException {
      return this.newInstanceOfProperty(Log4jPropertyKey.LOG_EVENT_FACTORY_CLASS_NAME, LogEventFactory.class);
   }

   @SingletonFactory
   public LogEventFactory defaultLogEventFactory(final ContextDataInjector injector, final Clock clock, final NanoClock nanoClock) {
      return Constants.isThreadLocalsEnabled()
         ? new ReusableLogEventFactory(injector, clock, nanoClock)
         : new DefaultLogEventFactory(injector, clock, nanoClock);
   }

   @SingletonFactory
   public InterpolatorFactory interpolatorFactory(@Namespace("Lookup") final Map<String, Supplier<StrLookup>> strLookupPlugins) {
      return defaultLookup -> new Interpolator(defaultLookup, strLookupPlugins);
   }

   @SingletonFactory
   public StrSubstitutor strSubstitutor(final InterpolatorFactory factory) {
      return new StrSubstitutor(factory.newInterpolator(null));
   }

   @SingletonFactory
   public ConfigurationFactory configurationFactory(final StrSubstitutor substitutor) {
      DefaultConfigurationFactory factory = new DefaultConfigurationFactory(this.injector);
      factory.setSubstitutor(substitutor);
      return factory;
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONFIG_MERGE_STRATEGY)
   @SingletonFactory
   @Ordered(100)
   public MergeStrategy systemPropertyMergeStrategy() throws ClassNotFoundException {
      return this.newInstanceOfProperty(Log4jPropertyKey.CONFIG_MERGE_STRATEGY, MergeStrategy.class);
   }

   @SingletonFactory
   public MergeStrategy defaultMergeStrategy() {
      return new DefaultMergeStrategy();
   }

   @ConditionalOnPropertyKey(key = Log4jPropertyKey.STATUS_DEFAULT_LEVEL)
   @SingletonFactory
   @Named("StatusLogger")
   @Ordered(100)
   public Level systemPropertyDefaultStatusLevel() {
      return Level.getLevel(this.properties.getStringProperty(Log4jPropertyKey.STATUS_DEFAULT_LEVEL));
   }

   @SingletonFactory
   @Named("StatusLogger")
   public Level defaultStatusLevel() {
      return Level.ERROR;
   }

   private <T> T newInstanceOfProperty(final PropertyKey propertyKey, final Class<T> supertype) throws ClassNotFoundException {
      String property = this.properties.getStringProperty(propertyKey);
      if (property == null) {
         throw new InjectException("No property defined for name " + propertyKey.toString());
      } else {
         return (T)this.injector.getInstance(this.classLoader.loadClass(property).asSubclass(supertype));
      }
   }

   private static Clock logSupportedPrecision(final Clock clock) {
      String support = clock instanceof PreciseClock ? "supports" : "does not support";
      LOGGER.debug("{} {} precise timestamps.", clock.getClass().getName(), support);
      return clock;
   }
}
