package org.apache.logging.log4j.core.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.properties.PropertiesConfiguration;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.impl.ReusableLogEventFactory;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Strings;

@Configurable(printObject = true)
@Plugin("logger")
public class LoggerConfig extends AbstractFilterable {
   public static final String ROOT = "root";
   private List<AppenderRef> appenderRefs = new ArrayList<>();
   private final AppenderControlArraySet appenders = new AppenderControlArraySet();
   private final String name;
   private LogEventFactory logEventFactory;
   private Level level;
   private boolean additive = true;
   private boolean includeLocation = true;
   private LoggerConfig parent;
   private Map<Property, Boolean> propertiesMap;
   private final List<Property> properties;
   private final boolean propertiesRequireLookup;
   private final Configuration config;
   private final ReliabilityStrategy reliabilityStrategy;

   @PluginFactory
   public static <B extends LoggerConfig.Builder<B>> B newBuilder() {
      return new LoggerConfig.Builder<B>().asBuilder();
   }

   public LoggerConfig() {
      this.logEventFactory = DefaultLogEventFactory.newInstance();
      this.level = Level.ERROR;
      this.name = "";
      this.properties = null;
      this.propertiesRequireLookup = false;
      this.config = null;
      this.reliabilityStrategy = new DefaultReliabilityStrategy(this);
   }

   public LoggerConfig(final String name, final Level level, final boolean additive) {
      this.logEventFactory = DefaultLogEventFactory.newInstance();
      this.name = name;
      this.level = level;
      this.additive = additive;
      this.properties = null;
      this.propertiesRequireLookup = false;
      this.config = null;
      this.reliabilityStrategy = new DefaultReliabilityStrategy(this);
   }

   protected LoggerConfig(
      final String name,
      final List<AppenderRef> appenders,
      final Filter filter,
      final Level level,
      final boolean additive,
      final Property[] properties,
      final Configuration config,
      final boolean includeLocation,
      final LogEventFactory logEventFactory
   ) {
      super(filter, null);
      this.logEventFactory = logEventFactory;
      this.name = name;
      this.appenderRefs = appenders;
      this.level = level;
      this.additive = additive;
      this.includeLocation = includeLocation;
      this.config = config;
      if (properties != null && properties.length > 0) {
         this.properties = List.of((Property[])properties.clone());
      } else {
         this.properties = null;
      }

      this.propertiesRequireLookup = containsPropertyRequiringLookup(properties);
      this.reliabilityStrategy = config.getReliabilityStrategy(this);
   }

   private static boolean containsPropertyRequiringLookup(final Property[] properties) {
      if (properties == null) {
         return false;
      }

      for (int i = 0; i < properties.length; i++) {
         if (properties[i].isValueNeedsLookup()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public Filter getFilter() {
      return super.getFilter();
   }

   public String getName() {
      return this.name;
   }

   public void setParent(final LoggerConfig parent) {
      this.parent = parent;
   }

   public LoggerConfig getParent() {
      return this.parent;
   }

   public void addAppender(final Appender appender, final Level level, final Filter filter) {
      this.appenders.add(new AppenderControl(appender, level, filter));
   }

   public void removeAppender(final String name) {
      AppenderControl removed = null;

      while ((removed = this.appenders.remove(name)) != null) {
         this.cleanupFilter(removed);
      }
   }

   public Map<String, Appender> getAppenders() {
      return this.appenders.asMap();
   }

   protected void clearAppenders() {
      do {
         AppenderControl[] original = this.appenders.clear();

         for (AppenderControl ctl : original) {
            this.cleanupFilter(ctl);
         }
      } while (!this.appenders.isEmpty());
   }

   private void cleanupFilter(final AppenderControl ctl) {
      Filter filter = ctl.getFilter();
      if (filter != null) {
         ctl.removeFilter(filter);
         filter.stop();
      }
   }

   public List<AppenderRef> getAppenderRefs() {
      return this.appenderRefs;
   }

   public void setLevel(final Level level) {
      this.level = level;
   }

   public Level getLevel() {
      return this.level == null ? (this.parent == null ? Level.ERROR : this.parent.getLevel()) : this.level;
   }

   public Level getExplicitLevel() {
      return this.level;
   }

   public LogEventFactory getLogEventFactory() {
      return this.logEventFactory;
   }

   public void setLogEventFactory(final LogEventFactory logEventFactory) {
      this.logEventFactory = logEventFactory;
   }

   public boolean isAdditive() {
      return this.additive;
   }

   public void setAdditive(final boolean additive) {
      this.additive = additive;
   }

   public boolean isIncludeLocation() {
      return this.includeLocation;
   }

   public List<Property> getPropertyList() {
      return this.properties;
   }

   public boolean isPropertiesRequireLookup() {
      return this.propertiesRequireLookup;
   }

   @PerformanceSensitive("allocation")
   public void log(final String loggerName, final String fqcn, final Marker marker, final Level level, final Message data, final Throwable t) {
      List<Property> props = this.getProperties(loggerName, fqcn, marker, level, data, t);
      LogEvent logEvent = this.logEventFactory.createEvent(loggerName, marker, fqcn, this.location(fqcn), level, data, props, t);

      try {
         this.log(logEvent, LoggerConfig.LoggerConfigPredicate.ALL);
      } finally {
         ReusableLogEventFactory.release(logEvent);
      }
   }

   private StackTraceElement location(final String fqcn) {
      return this.requiresLocation() ? StackLocatorUtil.calcLocation(fqcn) : null;
   }

   @PerformanceSensitive("allocation")
   public void log(
      final String loggerName,
      final String fqcn,
      final StackTraceElement location,
      final Marker marker,
      final Level level,
      final Message data,
      final Throwable t
   ) {
      List<Property> props = this.getProperties(loggerName, fqcn, marker, level, data, t);
      LogEvent logEvent = this.logEventFactory.createEvent(loggerName, marker, fqcn, location, level, data, props, t);

      try {
         this.log(logEvent, LoggerConfig.LoggerConfigPredicate.ALL);
      } finally {
         ReusableLogEventFactory.release(logEvent);
      }
   }

   private List<Property> getProperties(
      final String loggerName, final String fqcn, final Marker marker, final Level level, final Message data, final Throwable t
   ) {
      List<Property> snapshot = this.properties;
      return snapshot != null && this.propertiesRequireLookup ? this.getPropertiesWithLookups(loggerName, fqcn, marker, level, data, t, snapshot) : snapshot;
   }

   private List<Property> getPropertiesWithLookups(
      final String loggerName, final String fqcn, final Marker marker, final Level level, final Message data, final Throwable t, final List<Property> props
   ) {
      List<Property> results = new ArrayList<>(props.size());
      LogEvent event = Log4jLogEvent.newBuilder()
         .setMessage(data)
         .setMarker(marker)
         .setLevel(level)
         .setLoggerName(loggerName)
         .setLoggerFqcn(fqcn)
         .setThrown(t)
         .build();

      for (int i = 0; i < props.size(); i++) {
         Property prop = props.get(i);
         String value = prop.isValueNeedsLookup() ? this.config.getStrSubstitutor().replace(event, prop.getValue()) : prop.getValue();
         results.add(Property.createProperty(prop.getName(), value));
      }

      return results;
   }

   public void log(final LogEvent event) {
      this.log(event, LoggerConfig.LoggerConfigPredicate.ALL);
   }

   protected void log(final LogEvent event, final LoggerConfig.LoggerConfigPredicate predicate) {
      if (!this.isFiltered(event)) {
         this.processLogEvent(event, predicate);
      }
   }

   public ReliabilityStrategy getReliabilityStrategy() {
      return this.reliabilityStrategy;
   }

   private void processLogEvent(final LogEvent event, final LoggerConfig.LoggerConfigPredicate predicate) {
      event.setIncludeLocation(this.isIncludeLocation());
      if (predicate.allow(this)) {
         this.callAppenders(event);
      }

      this.logParent(event, predicate);
   }

   public boolean requiresLocation() {
      if (!this.includeLocation) {
         return false;
      }

      AppenderControl[] controls = this.appenders.get();
      LoggerConfig loggerConfig = this;

      while (loggerConfig != null) {
         for (AppenderControl control : controls) {
            if (control.getAppender().requiresLocation()) {
               return true;
            }
         }

         if (!loggerConfig.additive) {
            break;
         }

         loggerConfig = loggerConfig.parent;
         if (loggerConfig != null) {
            controls = loggerConfig.appenders.get();
         }
      }

      return false;
   }

   private void logParent(final LogEvent event, final LoggerConfig.LoggerConfigPredicate predicate) {
      if (this.additive && this.parent != null) {
         this.parent.log(event, predicate);
      }
   }

   @PerformanceSensitive("allocation")
   protected void callAppenders(final LogEvent event) {
      AppenderControl[] controls = this.appenders.get();

      for (int i = 0; i < controls.length; i++) {
         controls[i].callAppender(event);
      }
   }

   @Override
   public String toString() {
      return Strings.isEmpty(this.name) ? "root" : this.name;
   }

   @Deprecated
   public static LoggerConfig createLogger(
      final boolean additivity,
      final Level level,
      final String loggerName,
      final String includeLocation,
      final AppenderRef[] refs,
      final Property[] properties,
      final Configuration config,
      final Filter filter
   ) {
      String name = loggerName.equals("root") ? "" : loggerName;
      return new LoggerConfig(
         name,
         Arrays.asList(refs),
         filter,
         level,
         additivity,
         properties,
         config,
         includeLocation(includeLocation, config),
         config.getComponent(LogEventFactory.KEY)
      );
   }

   protected static boolean includeLocation(final String includeLocationConfigValue, final Configuration configuration) {
      if (includeLocationConfigValue == null) {
         LoggerContext context = null;
         if (configuration != null) {
            context = configuration.getLoggerContext();
         }

         return context != null ? !(context instanceof AsyncLoggerContext) : !AsyncLoggerContextSelector.isSelected();
      } else {
         return Boolean.parseBoolean(includeLocationConfigValue);
      }
   }

   protected final boolean hasAppenders() {
      return !this.appenders.isEmpty();
   }

   protected static LoggerConfig.LevelAndRefs getLevelAndRefs(
      final Level level, final AppenderRef[] refs, final String levelAndRefs, final Configuration config
   ) {
      LoggerConfig.LevelAndRefs result = new LoggerConfig.LevelAndRefs();
      if (levelAndRefs != null) {
         if (config instanceof PropertiesConfiguration) {
            if (level != null) {
               LOGGER.warn("Level is ignored when levelAndRefs syntax is used.");
            }

            if (refs != null && refs.length > 0) {
               LOGGER.warn("Appender references are ignored when levelAndRefs syntax is used");
            }

            String[] parts = Strings.splitList(levelAndRefs);
            result.level = Level.getLevel(parts[0]);
            if (parts.length > 1) {
               List<AppenderRef> refList = new ArrayList<>();
               Arrays.stream(parts).skip(1L).forEach(ref -> refList.add(AppenderRef.createAppenderRef(ref, null, null)));
               result.refs = refList;
            }
         } else {
            LOGGER.warn("levelAndRefs are only allowed in a properties configuration. The value is ignored.");
            result.level = level;
            result.refs = refs != null ? Arrays.asList(refs) : new ArrayList<>();
         }
      } else {
         result.level = level;
         result.refs = refs != null ? Arrays.asList(refs) : new ArrayList<>();
      }

      return result;
   }

   public static class Builder<B extends LoggerConfig.Builder<B>> implements org.apache.logging.log4j.core.util.Builder<LoggerConfig> {
      @PluginBuilderAttribute
      private Boolean additivity;
      private Level level;
      private String levelAndRefs;
      private String loggerName;
      private String includeLocation;
      private AppenderRef[] refs;
      private Property[] properties;
      private Configuration config;
      private Filter filter;
      private LogEventFactory logEventFactory;

      public boolean isAdditivity() {
         return this.additivity == null || this.additivity;
      }

      public B withAdditivity(final boolean additivity) {
         this.additivity = additivity;
         return this.asBuilder();
      }

      public Level getLevel() {
         return this.level;
      }

      public B withLevel(@PluginAttribute final Level level) {
         this.level = level;
         return this.asBuilder();
      }

      public String getLevelAndRefs() {
         return this.levelAndRefs;
      }

      public B withLevelAndRefs(@PluginAttribute final String levelAndRefs) {
         this.levelAndRefs = levelAndRefs;
         return this.asBuilder();
      }

      public String getLoggerName() {
         return this.loggerName;
      }

      public B withLoggerName(@Required(message = "Loggers cannot be configured without a name") @PluginAttribute final String name) {
         this.loggerName = name;
         return this.asBuilder();
      }

      public String getIncludeLocation() {
         return this.includeLocation;
      }

      public B withIncludeLocation(@PluginAttribute final String includeLocation) {
         this.includeLocation = includeLocation;
         return this.asBuilder();
      }

      public AppenderRef[] getRefs() {
         return this.refs;
      }

      public B withRefs(@PluginElement final AppenderRef[] refs) {
         this.refs = refs;
         return this.asBuilder();
      }

      public Property[] getProperties() {
         return this.properties;
      }

      public B withProperties(@PluginElement final Property[] properties) {
         this.properties = properties;
         return this.asBuilder();
      }

      public Configuration getConfig() {
         return this.config;
      }

      public B withConfig(@PluginConfiguration final Configuration config) {
         this.config = config;
         return this.asBuilder();
      }

      public Filter getFilter() {
         return this.filter;
      }

      public B withFilter(@PluginElement final Filter filter) {
         this.filter = filter;
         return this.asBuilder();
      }

      public LogEventFactory getLogEventFactory() {
         return this.logEventFactory;
      }

      @Inject
      public B setLogEventFactory(final LogEventFactory logEventFactory) {
         this.logEventFactory = logEventFactory;
         return this.asBuilder();
      }

      public LoggerConfig build() {
         String name = this.loggerName.equals("root") ? "" : this.loggerName;
         LoggerConfig.LevelAndRefs container = LoggerConfig.getLevelAndRefs(this.level, this.refs, this.levelAndRefs, this.config);
         boolean useLocation = LoggerConfig.includeLocation(this.includeLocation, this.config);
         return new LoggerConfig(
            name, container.refs, this.filter, container.level, this.isAdditivity(), this.properties, this.config, useLocation, this.logEventFactory
         );
      }

      public B asBuilder() {
         return (B)this;
      }
   }

   protected static class LevelAndRefs {
      public Level level;
      public List<AppenderRef> refs;
   }

   protected enum LoggerConfigPredicate {
      ALL {
         @Override
         boolean allow(final LoggerConfig config) {
            return true;
         }
      },
      ASYNCHRONOUS_ONLY {
         @Override
         boolean allow(final LoggerConfig config) {
            return config instanceof AsyncLoggerConfig;
         }
      },
      SYNCHRONOUS_ONLY {
         @Override
         boolean allow(final LoggerConfig config) {
            return !ASYNCHRONOUS_ONLY.allow(config);
         }
      };

      abstract boolean allow(LoggerConfig config);
   }

   @Configurable(printObject = true)
   @Plugin("root")
   public static class RootLogger extends LoggerConfig {
      @PluginFactory
      public static <B extends LoggerConfig.RootLogger.Builder<B>> B newRootBuilder() {
         return new LoggerConfig.RootLogger.Builder<B>().asBuilder();
      }

      @Deprecated
      public static LoggerConfig createLogger(
         final String additivity,
         final Level level,
         final String includeLocation,
         final AppenderRef[] refs,
         final Property[] properties,
         final Configuration config,
         final Filter filter
      ) {
         List<AppenderRef> appenderRefs = Arrays.asList(refs);
         Level actualLevel = level == null ? Level.ERROR : level;
         boolean additive = Booleans.parseBoolean(additivity, true);
         return new LoggerConfig(
            "",
            appenderRefs,
            filter,
            actualLevel,
            additive,
            properties,
            config,
            includeLocation(includeLocation, config),
            config.getComponent(LogEventFactory.KEY)
         );
      }

      public static class Builder<B extends LoggerConfig.RootLogger.Builder<B>> implements org.apache.logging.log4j.core.util.Builder<LoggerConfig> {
         private boolean additivity;
         private Level level;
         private String levelAndRefs;
         private String includeLocation;
         private AppenderRef[] refs;
         private Property[] properties;
         private Configuration config;
         private Filter filter;
         private LogEventFactory logEventFactory;

         public boolean isAdditivity() {
            return this.additivity;
         }

         public B withAdditivity(@PluginAttribute final boolean additivity) {
            this.additivity = additivity;
            return this.asBuilder();
         }

         public Level getLevel() {
            return this.level;
         }

         public B withLevel(@PluginAttribute final Level level) {
            this.level = level;
            return this.asBuilder();
         }

         public String getLevelAndRefs() {
            return this.levelAndRefs;
         }

         public B withLevelAndRefs(@PluginAttribute final String levelAndRefs) {
            this.levelAndRefs = levelAndRefs;
            return this.asBuilder();
         }

         public String getIncludeLocation() {
            return this.includeLocation;
         }

         public B withIncludeLocation(@PluginAttribute final String includeLocation) {
            this.includeLocation = includeLocation;
            return this.asBuilder();
         }

         public AppenderRef[] getRefs() {
            return this.refs;
         }

         public B withRefs(@PluginElement final AppenderRef[] refs) {
            this.refs = refs;
            return this.asBuilder();
         }

         public Property[] getProperties() {
            return this.properties;
         }

         public B withProperties(@PluginElement final Property[] properties) {
            this.properties = properties;
            return this.asBuilder();
         }

         public Configuration getConfig() {
            return this.config;
         }

         public B withConfig(@PluginConfiguration final Configuration config) {
            this.config = config;
            return this.asBuilder();
         }

         public Filter getFilter() {
            return this.filter;
         }

         public B withFilter(@PluginElement final Filter filter) {
            this.filter = filter;
            return this.asBuilder();
         }

         public LogEventFactory getLogEventFactory() {
            return this.logEventFactory;
         }

         @Inject
         public B withLogEventFactory(final LogEventFactory logEventFactory) {
            this.logEventFactory = logEventFactory;
            return this.asBuilder();
         }

         public LoggerConfig build() {
            LoggerConfig.LevelAndRefs container = LoggerConfig.getLevelAndRefs(this.level, this.refs, this.levelAndRefs, this.config);
            return new LoggerConfig(
               "",
               container.refs,
               this.filter,
               container.level,
               this.additivity,
               this.properties,
               this.config,
               LoggerConfig.includeLocation(this.includeLocation, this.config),
               this.logEventFactory
            );
         }

         public B asBuilder() {
            return (B)this;
         }
      }
   }
}
