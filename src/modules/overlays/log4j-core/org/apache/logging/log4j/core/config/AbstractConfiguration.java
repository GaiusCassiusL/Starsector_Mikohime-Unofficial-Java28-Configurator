package org.apache.logging.log4j.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.Version;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.async.AsyncLoggerConfigDelegate;
import org.apache.logging.log4j.core.async.AsyncLoggerConfigDisruptor;
import org.apache.logging.log4j.core.async.AsyncWaitStrategyFactory;
import org.apache.logging.log4j.core.async.AsyncWaitStrategyFactoryConfig;
import org.apache.logging.log4j.core.config.arbiters.Arbiter;
import org.apache.logging.log4j.core.config.arbiters.SelectArbiter;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.lookup.ConfigurationStrSubstitutor;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.RuntimeStrSubstitutor;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.core.script.ScriptManagerFactory;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.core.util.Watcher;
import org.apache.logging.log4j.core.util.WatcherFactory;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.NameUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.ServiceRegistry;

public abstract class AbstractConfiguration extends AbstractFilterable implements Configuration {
   protected Node rootNode = new Node();
   protected final List<ConfigurationListener> listeners = new CopyOnWriteArrayList<>();
   protected final List<String> pluginPackages = new ArrayList<>();
   protected PluginNamespace corePlugins;
   protected boolean isShutdownHookEnabled = true;
   protected long shutdownTimeoutMillis;
   protected ScriptManager scriptManager;
   protected final Injector injector;
   private Advertiser advertiser = new DefaultAdvertiser();
   private Node advertiserNode;
   private Object advertisement;
   private String name;
   private ConcurrentMap<String, Appender> appenders = new ConcurrentHashMap<>();
   private ConcurrentMap<String, LoggerConfig> loggerConfigs = new ConcurrentHashMap<>();
   private List<CustomLevelConfig> customLevels = List.of();
   private final ConcurrentMap<String, String> properties = new ConcurrentHashMap<>();
   private final InterpolatorFactory interpolatorFactory;
   private final Interpolator tempLookup;
   private final StrSubstitutor runtimeStrSubstitutor;
   private final StrSubstitutor configurationStrSubstitutor;
   private LoggerConfig root = new LoggerConfig();
   private final ConcurrentMap<String, Object> componentMap = new ConcurrentHashMap<>();
   private final ConfigurationSource configurationSource;
   private final ConfigurationScheduler configurationScheduler;
   private final WatchManager watchManager;
   private AsyncLoggerConfigDisruptor asyncLoggerConfigDisruptor;
   private AsyncWaitStrategyFactory asyncWaitStrategyFactory;
   private final WeakReference<LoggerContext> loggerContext;
   private PropertyEnvironment contextProperties;

   protected AbstractConfiguration(final LoggerContext loggerContext, final ConfigurationSource configurationSource) {
      this.loggerContext = new WeakReference<>(loggerContext);
      this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource is null");
      if (loggerContext != null) {
         this.injector = loggerContext.getInjector();
         this.contextProperties = loggerContext.getProperties();
      } else {
         this.injector = DI.createInjector();
         this.injector.init();
         this.contextProperties = PropertiesUtil.getProperties();
      }

      this.componentMap.put("ContextProperties", this.properties);
      this.interpolatorFactory = (InterpolatorFactory)this.injector.getInstance(InterpolatorFactory.class);
      this.tempLookup = this.interpolatorFactory.newInterpolator(new PropertiesLookup(this.properties));
      this.tempLookup.setLoggerContext(loggerContext);
      this.runtimeStrSubstitutor = new RuntimeStrSubstitutor(this.tempLookup);
      this.configurationStrSubstitutor = new ConfigurationStrSubstitutor(this.runtimeStrSubstitutor);
      this.configurationScheduler = (ConfigurationScheduler)this.injector.getInstance(ConfigurationScheduler.class);
      this.watchManager = (WatchManager)this.injector.getInstance(WatchManager.class);
      this.setState(LifeCycle.State.INITIALIZING);
   }

   @Override
   public ConfigurationSource getConfigurationSource() {
      return this.configurationSource;
   }

   @Override
   public List<String> getPluginPackages() {
      return this.pluginPackages;
   }

   @Override
   public Map<String, String> getProperties() {
      return this.properties;
   }

   @Override
   public ScriptManager getScriptManager() {
      return this.scriptManager;
   }

   public void setScriptManager(final ScriptManager scriptManager) {
      this.scriptManager = scriptManager;
      this.injector.registerBinding(ScriptManager.KEY, this::getScriptManager);
   }

   public PluginNamespace getCorePlugins() {
      return this.corePlugins;
   }

   public void setCorePlugins(final PluginNamespace corePlugins) {
      this.corePlugins = corePlugins;
      this.injector.registerBinding(Core.PLUGIN_NAMESPACE_KEY, this::getCorePlugins);
   }

   @Override
   public WatchManager getWatchManager() {
      return this.watchManager;
   }

   @Override
   public ConfigurationScheduler getScheduler() {
      return this.configurationScheduler;
   }

   public Node getRootNode() {
      return this.rootNode;
   }

   @Override
   public AsyncLoggerConfigDelegate getAsyncLoggerConfigDelegate() {
      if (this.asyncLoggerConfigDisruptor == null) {
         this.asyncLoggerConfigDisruptor = new AsyncLoggerConfigDisruptor(this.asyncWaitStrategyFactory);
      }

      return this.asyncLoggerConfigDisruptor;
   }

   @Override
   public AsyncWaitStrategyFactory getAsyncWaitStrategyFactory() {
      return this.asyncWaitStrategyFactory;
   }

   @Override
   public void initialize() {
      LOGGER.debug("{} initializing configuration {}", Version.getProductString(), this);
      this.injector.registerBinding(Configuration.KEY, () -> this);
      this.runtimeStrSubstitutor.setConfiguration(this);
      this.configurationStrSubstitutor.setConfiguration(this);
      this.initializeScriptManager();
      this.corePlugins = (PluginNamespace)this.injector.getInstance(Core.PLUGIN_NAMESPACE_KEY);
      PluginNamespace levelPlugins = (PluginNamespace)this.injector.getInstance(new Key<PluginNamespace>() {});
      levelPlugins.forEach(type -> {
         Class<?> pluginClass = type.getPluginClass();

         try {
            Class.forName(pluginClass.getName(), true, pluginClass.getClassLoader());
         } catch (Exception e) {
            LOGGER.error("Unable to initialize {} due to {}", pluginClass.getName(), e.getClass().getSimpleName(), e);
         }
      });
      this.setup();
      this.setupAdvertisement();
      this.doConfigure();
      this.setState(LifeCycle.State.INITIALIZED);
      LOGGER.debug("Configuration {} initialized", this);
   }

   private void initializeScriptManager() {
      try {
         ServiceRegistry.getInstance()
            .getServices(ScriptManagerFactory.class, MethodHandles.lookup(), null)
            .stream()
            .findFirst()
            .ifPresent(factory -> this.setScriptManager(factory.createScriptManager(this, this.getWatchManager())));
      } catch (LinkageError | Exception e) {
         LOGGER.info("Cannot initialize scripting support because this JRE does not support it.", e);
      }
   }

   protected void initializeWatchers(final Reconfigurable reconfigurable, final ConfigurationSource configSource, final int monitorIntervalSeconds) {
      if (configSource != null && (configSource.getFile() != null || configSource.getURL() != null)) {
         if (monitorIntervalSeconds > 0) {
            this.watchManager.setIntervalSeconds(monitorIntervalSeconds);
            if (configSource.getFile() != null) {
               Source cfgSource = new Source(configSource);
               long lastModified = configSource.getFile().lastModified();
               ConfigurationFileWatcher watcher = new ConfigurationFileWatcher(this, reconfigurable, this.listeners, lastModified);
               this.watchManager.watch(cfgSource, watcher);
            } else if (configSource.getURL() != null) {
               this.monitorSource(reconfigurable, configSource);
            }
         } else if (this.watchManager.hasEventListeners() && configSource.getURL() != null && monitorIntervalSeconds >= 0) {
            this.monitorSource(reconfigurable, configSource);
         }
      }
   }

   private void monitorSource(final Reconfigurable reconfigurable, final ConfigurationSource configSource) {
      if (configSource.getLastModified() > 0L) {
         Source cfgSource = new Source(configSource);
         Key<WatcherFactory> key = Key.forClass(WatcherFactory.class);
         this.injector
            .registerBindingIfAbsent(key, Lazy.lazy(() -> new WatcherFactory((PluginNamespace)this.injector.getInstance(Watcher.PLUGIN_CATEGORY_KEY))));
         Watcher watcher = ((WatcherFactory)this.injector.getInstance(key))
            .newWatcher(cfgSource, this, reconfigurable, this.listeners, configSource.getLastModified());
         if (watcher != null) {
            this.watchManager.watch(cfgSource, watcher);
         }
      } else {
         LOGGER.info("{} does not support dynamic reconfiguration", configSource.getURI());
      }
   }

   @Override
   public void start() {
      if (this.getState().equals(LifeCycle.State.INITIALIZING)) {
         this.initialize();
      }

      LOGGER.debug("Starting configuration {}", this);
      this.setStarting();
      if (this.watchManager.getIntervalSeconds() >= 0) {
         this.watchManager.start();
      }

      if (this.hasAsyncLoggers()) {
         this.asyncLoggerConfigDisruptor.start();
      }

      Set<LoggerConfig> alreadyStarted = new HashSet<>();

      for (LoggerConfig logger : this.loggerConfigs.values()) {
         logger.start();
         alreadyStarted.add(logger);
      }

      for (Appender appender : this.appenders.values()) {
         appender.start();
      }

      if (!alreadyStarted.contains(this.root)) {
         this.root.start();
      }

      super.start();
      LOGGER.debug("Started configuration {} OK.", this);
   }

   private boolean hasAsyncLoggers() {
      if (this.root instanceof AsyncLoggerConfig) {
         return true;
      }

      for (LoggerConfig logger : this.loggerConfigs.values()) {
         if (logger instanceof AsyncLoggerConfig) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      this.setStopping();
      super.stop(timeout, timeUnit, false);
      LOGGER.trace("Stopping {}...", this);

      for (LoggerConfig loggerConfig : this.loggerConfigs.values()) {
         loggerConfig.getReliabilityStrategy().beforeStopConfiguration(this);
      }

      this.root.getReliabilityStrategy().beforeStopConfiguration(this);
      String cls = this.getClass().getSimpleName();
      LOGGER.trace("{} notified {} ReliabilityStrategies that config will be stopped.", cls, this.loggerConfigs.size() + 1);
      if (!this.loggerConfigs.isEmpty()) {
         LOGGER.trace("{} stopping {} LoggerConfigs.", cls, this.loggerConfigs.size());

         for (LoggerConfig logger : this.loggerConfigs.values()) {
            logger.stop(timeout, timeUnit);
         }
      }

      LOGGER.trace("{} stopping root LoggerConfig.", cls);
      if (!this.root.isStopped()) {
         this.root.stop(timeout, timeUnit);
      }

      if (this.hasAsyncLoggers()) {
         LOGGER.trace("{} stopping AsyncLoggerConfigDisruptor.", cls);
         this.asyncLoggerConfigDisruptor.stop(timeout, timeUnit);
      }

      LOGGER.trace("{} notifying ReliabilityStrategies that appenders will be stopped.", cls);

      for (LoggerConfig loggerConfig : this.loggerConfigs.values()) {
         loggerConfig.getReliabilityStrategy().beforeStopAppenders();
      }

      this.root.getReliabilityStrategy().beforeStopAppenders();
      Appender[] array = this.appenders.values().toArray(Appender.EMPTY_ARRAY);
      List<Appender> async = this.getAsyncAppenders(array);
      if (!async.isEmpty()) {
         LOGGER.trace("{} stopping {} AsyncAppenders.", cls, async.size());

         for (Appender appender : async) {
            appender.stop(timeout, timeUnit);
         }
      }

      LOGGER.trace("{} stopping remaining Appenders.", cls);
      int appenderCount = 0;

      for (int i = array.length - 1; i >= 0; i--) {
         if (array[i].isStarted()) {
            array[i].stop(timeout, timeUnit);
            appenderCount++;
         }
      }

      LOGGER.trace("{} stopped {} remaining Appenders.", cls, appenderCount);
      LOGGER.trace("{} cleaning Appenders from {} LoggerConfigs.", cls, this.loggerConfigs.size() + 1);

      for (LoggerConfig loggerConfig : this.loggerConfigs.values()) {
         loggerConfig.clearAppenders();
      }

      this.root.clearAppenders();
      if (this.watchManager.isStarted()) {
         this.watchManager.stop(timeout, timeUnit);
      }

      this.configurationScheduler.stop(timeout, timeUnit);
      if (this.advertiser != null && this.advertisement != null) {
         this.advertiser.unadvertise(this.advertisement);
      }

      this.setStopped();
      LOGGER.debug("Stopped {} OK", this);
      return true;
   }

   private List<Appender> getAsyncAppenders(final Appender[] all) {
      List<Appender> result = new ArrayList<>();

      for (int i = all.length - 1; i >= 0; i--) {
         if (all[i] instanceof AsyncAppender) {
            result.add(all[i]);
         }
      }

      return result;
   }

   @Override
   public boolean isShutdownHookEnabled() {
      return this.isShutdownHookEnabled;
   }

   @Override
   public long getShutdownTimeoutMillis() {
      return this.shutdownTimeoutMillis;
   }

   public void setup() {
   }

   protected Level getDefaultStatus() {
      return (Level)this.injector.getInstance(Constants.DEFAULT_STATUS_LEVEL_KEY);
   }

   protected void createAdvertiser(final String advertiserString, final ConfigurationSource configSource, final byte[] buffer, final String contentType) {
      if (advertiserString != null) {
         Node node = new Node(null, advertiserString, null);
         Map<String, String> attributes = node.getAttributes();
         attributes.put("content", new String(buffer));
         attributes.put("contentType", contentType);
         attributes.put("name", "configuration");
         if (configSource.getLocation() != null) {
            attributes.put("location", configSource.getLocation());
         }

         this.advertiserNode = node;
      }
   }

   private void setupAdvertisement() {
      if (this.advertiserNode != null) {
         String nodeName = this.advertiserNode.getName();
         PluginType<?> type = this.corePlugins.get(nodeName);
         if (type != null) {
            this.advertiser = (Advertiser)this.injector.getInstance(type.getPluginClass().asSubclass(Advertiser.class));
            this.advertisement = this.advertiser.advertise(this.advertiserNode.getAttributes());
         }
      }
   }

   @Override
   public <T> T getComponent(final String componentName) {
      return (T)Cast.cast(this.componentMap.get(componentName));
   }

   @Override
   public <T> Supplier<T> getFactory(final Key<T> key) {
      return this.injector.getFactory(key);
   }

   @Override
   public void addComponent(final String componentName, final Object obj) {
      this.componentMap.putIfAbsent(componentName, obj);
   }

   protected void preConfigure(final Node node) {
      try {
         for (Node child : node.getChildren()) {
            if (child.getType() == null) {
               LOGGER.error("Unable to locate plugin type for {}", child.getName());
            } else {
               Class<?> clazz = child.getType().getPluginClass();
               if (clazz.isAnnotationPresent(Scheduled.class)) {
                  this.configurationScheduler.incrementScheduledItems();
               }

               this.preConfigure(child);
            }
         }
      } catch (Exception ex) {
         LOGGER.error("Error capturing node data for node {}", node.getName(), ex);
      }
   }

   protected void processConditionals(final Node node) {
      try {
         List<Node> addList = new ArrayList<>();
         List<Node> removeList = new ArrayList<>();

         for (Node child : node.getChildren()) {
            PluginType<?> type = child.getType();
            if (type != null && "Arbiter".equals(type.getElementType())) {
               Class<?> clazz = type.getPluginClass();
               if (SelectArbiter.class.isAssignableFrom(clazz)) {
                  removeList.add(child);
                  addList.addAll(this.processSelect(child, type));
               } else if (Arbiter.class.isAssignableFrom(clazz)) {
                  removeList.add(child);

                  try {
                     Arbiter condition = (Arbiter)this.injector.configure(child);
                     if (condition.isCondition()) {
                        addList.addAll(child.getChildren());
                        this.processConditionals(child);
                     }
                  } catch (Exception inner) {
                     LOGGER.error("Exception processing {}: Ignoring and including children", type.getPluginClass());
                     this.processConditionals(child);
                  }
               } else {
                  LOGGER.error("Encountered Condition Plugin that does not implement Condition: {}", child.getName());
                  this.processConditionals(child);
               }
            } else {
               this.processConditionals(child);
            }
         }

         if (!removeList.isEmpty()) {
            List<Node> children = node.getChildren();
            children.removeAll(removeList);
            children.addAll(addList);

            for (Node grandChild : addList) {
               grandChild.setParent(node);
            }
         }
      } catch (Exception ex) {
         LOGGER.error("Error capturing node data for node {}", node.getName(), ex);
      }
   }

   protected List<Node> processSelect(final Node selectNode, final PluginType<?> type) {
      List<Node> addList = new ArrayList<>();
      SelectArbiter select = (SelectArbiter)this.injector.configure(selectNode);
      List<Arbiter> conditions = new ArrayList<>();

      for (Node child : selectNode.getChildren()) {
         PluginType<?> nodeType = child.getType();
         if (nodeType != null) {
            if (Arbiter.class.isAssignableFrom(nodeType.getPluginClass())) {
               Arbiter condition = (Arbiter)this.injector.configure(child);
               conditions.add(condition);
            } else {
               LOGGER.error("Invalid Node {} for Select. Must be a Condition", child.getName());
            }
         } else {
            LOGGER.error("No PluginType for node {}", child.getName());
         }
      }

      Arbiter condition = select.evaluateConditions(conditions);
      if (condition != null) {
         for (Node child : selectNode.getChildren()) {
            if (condition == child.getObject()) {
               addList.addAll(child.getChildren());
               this.processConditionals(child);
            }
         }
      }

      return addList;
   }

   protected void doConfigure() {
      this.injector.registerBinding(Keys.SUBSTITUTOR_KEY, () -> this.configurationStrSubstitutor::replace);
      this.injector.registerBinding(LoggerContext.KEY, () -> this.loggerContext);
      this.processConditionals(this.rootNode);
      this.preConfigure(this.rootNode);
      this.configurationScheduler.start();
      if (this.rootNode.hasChildren() && ((Node)this.rootNode.getChildren().get(0)).getName().equalsIgnoreCase("Properties")) {
         Node first = (Node)this.rootNode.getChildren().get(0);
         this.createConfiguration(first, null);
         if (first.getObject() != null) {
            StrLookup lookup = (StrLookup)first.getObject();
            if (lookup instanceof LoggerContextAware) {
               ((LoggerContextAware)lookup).setLoggerContext(this.loggerContext.get());
            }

            this.runtimeStrSubstitutor.setVariableResolver(lookup);
            this.configurationStrSubstitutor.setVariableResolver(lookup);
         }
      } else {
         Map<String, String> map = this.getComponent("ContextProperties");
         StrLookup lookup = map == null ? null : new PropertiesLookup(map);
         Interpolator interpolator = this.interpolatorFactory.newInterpolator(lookup);
         interpolator.setLoggerContext(this.loggerContext.get());
         this.runtimeStrSubstitutor.setVariableResolver(interpolator);
         this.configurationStrSubstitutor.setVariableResolver(interpolator);
      }

      boolean setLoggers = false;
      boolean setRoot = false;

      for (Node child : this.rootNode.getChildren()) {
         if (child.getName().equalsIgnoreCase("Properties")) {
            if (this.tempLookup == this.runtimeStrSubstitutor.getVariableResolver()) {
               LOGGER.error("Properties declaration must be the first element in the configuration");
            }
         } else {
            this.createConfiguration(child, null);
            if (child.getObject() != null) {
               if (child.getName().equalsIgnoreCase("Scripts")) {
                  if (this.scriptManager != null) {
                     this.scriptManager.addScripts(child);
                  }
               } else if (child.getName().equalsIgnoreCase("Appenders")) {
                  this.appenders = (ConcurrentMap<String, Appender>)child.getObject();
               } else if (child.isInstanceOf(Filter.class)) {
                  this.addFilter((Filter)child.getObject(Filter.class));
               } else if (child.getName().equalsIgnoreCase("Loggers")) {
                  Loggers l = (Loggers)child.getObject();
                  this.loggerConfigs = l.getMap();
                  setLoggers = true;
                  if (l.getRoot() != null) {
                     this.root = l.getRoot();
                     setRoot = true;
                  }
               } else if (child.getName().equalsIgnoreCase("CustomLevels")) {
                  this.customLevels = ((CustomLevels)child.getObject(CustomLevels.class)).getCustomLevels();
               } else if (child.isInstanceOf(CustomLevelConfig.class)) {
                  List<CustomLevelConfig> copy = new ArrayList<>(this.customLevels);
                  copy.add((CustomLevelConfig)child.getObject(CustomLevelConfig.class));
                  this.customLevels = copy;
               } else if (child.isInstanceOf(AsyncWaitStrategyFactoryConfig.class)) {
                  AsyncWaitStrategyFactoryConfig awsfc = (AsyncWaitStrategyFactoryConfig)child.getObject(AsyncWaitStrategyFactoryConfig.class);
                  this.asyncWaitStrategyFactory = awsfc.createWaitStrategyFactory();
               } else {
                  List<String> expected = Arrays.asList("\"Appenders\"", "\"Loggers\"", "\"Properties\"", "\"Scripts\"", "\"CustomLevels\"");
                  LOGGER.error(
                     "Unknown object \"{}\" of type {} is ignored: try nesting it inside one of: {}.",
                     child.getName(),
                     child.getObject().getClass().getName(),
                     expected
                  );
               }
            }
         }
      }

      if (!setLoggers) {
         LOGGER.warn("No Loggers were configured, using default. Is the Loggers element missing?");
         this.setToDefault();
      } else {
         if (!setRoot) {
            LOGGER.warn("No Root logger was configured, creating default ERROR-level Root logger with Console appender");
            this.setToDefault();
         }

         for (Entry<String, LoggerConfig> entry : this.loggerConfigs.entrySet()) {
            LoggerConfig loggerConfig = entry.getValue();

            for (AppenderRef ref : loggerConfig.getAppenderRefs()) {
               Appender app = this.appenders.get(ref.getRef());
               if (app != null) {
                  loggerConfig.addAppender(app, ref.getLevel(), ref.getFilter());
               } else {
                  LOGGER.error("Unable to locate appender \"{}\" for logger config \"{}\"", ref.getRef(), loggerConfig);
               }
            }
         }

         this.setParents();
      }
   }

   protected void setToDefault() {
      this.setName("Default@" + Integer.toHexString(this.hashCode()));
      Layout layout = PatternLayout.newBuilder().setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n").setConfiguration(this).build();
      Appender appender = ConsoleAppender.createDefaultAppenderForLayout(layout);
      appender.start();
      this.addAppender(appender);
      LoggerConfig rootLoggerConfig = this.getRootLogger();
      rootLoggerConfig.addAppender(appender, null, null);
      Level defaultLevel = Level.ERROR;
      String levelName = this.contextProperties.getStringProperty(Log4jPropertyKey.CONFIG_DEFAULT_LEVEL, defaultLevel.name());
      Level level = Level.valueOf(levelName);
      rootLoggerConfig.setLevel(level != null ? level : defaultLevel);
   }

   public void setName(final String name) {
      this.name = name;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public void addListener(final ConfigurationListener listener) {
      this.listeners.add(listener);
   }

   @Override
   public void removeListener(final ConfigurationListener listener) {
      this.listeners.remove(listener);
   }

   @Override
   public <T extends Appender> T getAppender(final String appenderName) {
      return (T)(appenderName != null ? Cast.cast(this.appenders.get(appenderName)) : null);
   }

   @Override
   public Map<String, Appender> getAppenders() {
      return this.appenders;
   }

   @Override
   public void addAppender(final Appender appender) {
      if (appender != null) {
         this.appenders.putIfAbsent(appender.getName(), appender);
      }
   }

   @Override
   public StrSubstitutor getStrSubstitutor() {
      return this.runtimeStrSubstitutor;
   }

   @Override
   public StrSubstitutor getConfigurationStrSubstitutor() {
      return this.configurationStrSubstitutor;
   }

   @Override
   public void setAdvertiser(final Advertiser advertiser) {
      this.advertiser = advertiser;
   }

   @Override
   public Advertiser getAdvertiser() {
      return this.advertiser;
   }

   @Override
   public ReliabilityStrategy getReliabilityStrategy(final LoggerConfig loggerConfig) {
      return ReliabilityStrategyFactory.getReliabilityStrategy(loggerConfig);
   }

   @Override
   public synchronized void addLoggerAppender(final Logger logger, final Appender appender) {
      if (appender != null && logger != null) {
         String loggerName = logger.getName();
         this.appenders.putIfAbsent(appender.getName(), appender);
         LoggerConfig lc = this.getLoggerConfig(loggerName);
         if (lc.getName().equals(loggerName)) {
            lc.addAppender(appender, null, null);
         } else {
            LoggerConfig nlc = new LoggerConfig(loggerName, lc.getLevel(), lc.isAdditive());
            nlc.addAppender(appender, null, null);
            nlc.setParent(lc);
            this.loggerConfigs.putIfAbsent(loggerName, nlc);
            this.setParents();
            logger.getContext().updateLoggers();
         }
      }
   }

   @Override
   public synchronized void addLoggerFilter(final Logger logger, final Filter filter) {
      String loggerName = logger.getName();
      LoggerConfig lc = this.getLoggerConfig(loggerName);
      if (lc.getName().equals(loggerName)) {
         lc.addFilter(filter);
      } else {
         LoggerConfig nlc = new LoggerConfig(loggerName, lc.getLevel(), lc.isAdditive());
         nlc.addFilter(filter);
         nlc.setParent(lc);
         this.loggerConfigs.putIfAbsent(loggerName, nlc);
         this.setParents();
         logger.getContext().updateLoggers();
      }
   }

   @Override
   public synchronized void setLoggerAdditive(final Logger logger, final boolean additive) {
      String loggerName = logger.getName();
      LoggerConfig lc = this.getLoggerConfig(loggerName);
      if (lc.getName().equals(loggerName)) {
         lc.setAdditive(additive);
      } else {
         LoggerConfig nlc = new LoggerConfig(loggerName, lc.getLevel(), additive);
         nlc.setParent(lc);
         this.loggerConfigs.putIfAbsent(loggerName, nlc);
         this.setParents();
         logger.getContext().updateLoggers();
      }
   }

   public synchronized void removeAppender(final String appenderName) {
      for (LoggerConfig logger : this.loggerConfigs.values()) {
         logger.removeAppender(appenderName);
      }

      Appender app = appenderName != null ? this.appenders.remove(appenderName) : null;
      if (app != null) {
         app.stop();
      }
   }

   @Override
   public List<CustomLevelConfig> getCustomLevels() {
      return Collections.unmodifiableList(this.customLevels);
   }

   @Override
   public LoggerConfig getLoggerConfig(final String loggerName) {
      LoggerConfig loggerConfig = this.loggerConfigs.get(loggerName);
      if (loggerConfig != null) {
         return loggerConfig;
      }

      String substr = loggerName;

      while ((substr = NameUtil.getSubName(substr)) != null) {
         loggerConfig = this.loggerConfigs.get(substr);
         if (loggerConfig != null) {
            return loggerConfig;
         }
      }

      return this.root;
   }

   @Override
   public LoggerContext getLoggerContext() {
      return this.loggerContext.get();
   }

   @Override
   public LoggerConfig getRootLogger() {
      return this.root;
   }

   @Override
   public Map<String, LoggerConfig> getLoggers() {
      return Collections.unmodifiableMap(this.loggerConfigs);
   }

   public LoggerConfig getLogger(final String loggerName) {
      return this.loggerConfigs.get(loggerName);
   }

   @Override
   public synchronized void addLogger(final String loggerName, final LoggerConfig loggerConfig) {
      this.loggerConfigs.putIfAbsent(loggerName, loggerConfig);
      this.setParents();
   }

   @Override
   public synchronized void removeLogger(final String loggerName) {
      this.loggerConfigs.remove(loggerName);
      this.setParents();
   }

   @Override
   public void createConfiguration(final Node node, final LogEvent event) {
      Function<String, String> stringSubstitutionStrategy;
      if (event == null) {
         stringSubstitutionStrategy = this.configurationStrSubstitutor::replace;
      } else {
         stringSubstitutionStrategy = str -> this.runtimeStrSubstitutor.replace(event, str);
      }

      Injector injector = this.injector.copy().registerBinding(Keys.SUBSTITUTOR_KEY, () -> stringSubstitutionStrategy);
      injector.configure(node);
   }

   public Object createPluginObject(final Node node) {
      if (this.getState().equals(LifeCycle.State.INITIALIZING)) {
         Injector injector = this.injector.copy().registerBinding(Keys.SUBSTITUTOR_KEY, () -> this.configurationStrSubstitutor::replace);
         return injector.configure(node);
      } else {
         LOGGER.warn("Plugin Object creation is not allowed after initialization");
         return null;
      }
   }

   @Deprecated
   public Object createPluginObject(final PluginType<?> type, final Node node) {
      return this.createPluginObject(node);
   }

   private void setParents() {
      for (Entry<String, LoggerConfig> entry : this.loggerConfigs.entrySet()) {
         LoggerConfig logger = entry.getValue();
         String key = entry.getKey();
         if (!key.isEmpty()) {
            int i = key.lastIndexOf(46);
            if (i > 0) {
               key = key.substring(0, i);
               LoggerConfig parent = this.getLoggerConfig(key);
               if (parent == null) {
                  parent = this.root;
               }

               logger.setParent(parent);
            } else {
               logger.setParent(this.root);
            }
         }
      }
   }

   @Deprecated(since = "3.0.0")
   protected static byte[] toByteArray(final InputStream is) throws IOException {
      return is.readAllBytes();
   }

   @Override
   public NanoClock getNanoClock() {
      return (NanoClock)this.injector.getInstance(NanoClock.class);
   }

   @Override
   public void setNanoClock(final NanoClock nanoClock) {
      this.injector.registerBinding(NanoClock.KEY, () -> nanoClock);
   }
}
