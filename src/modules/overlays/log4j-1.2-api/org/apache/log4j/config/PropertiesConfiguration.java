package org.apache.log4j.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.builders.BuilderManager;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LifeCycle.State;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.util.LoaderUtil;

public class PropertiesConfiguration extends Log4j1Configuration {
   private static final String CATEGORY_PREFIX = "log4j.category.";
   private static final String LOGGER_PREFIX = "log4j.logger.";
   private static final String ADDITIVITY_PREFIX = "log4j.additivity.";
   private static final String ROOT_CATEGORY_PREFIX = "log4j.rootCategory";
   private static final String ROOT_LOGGER_PREFIX = "log4j.rootLogger";
   private static final String APPENDER_PREFIX = "log4j.appender.";
   private static final String LOGGER_REF = "logger-ref";
   private static final String ROOT_REF = "root-ref";
   private static final String APPENDER_REF_TAG = "appender-ref";
   private static final String RESET_KEY = "log4j.reset";
   public static final String THRESHOLD_KEY = "log4j.threshold";
   public static final String DEBUG_KEY = "log4j.debug";
   private static final String INTERNAL_ROOT_NAME = "root";
   private final Map<String, Appender> registry = new HashMap<>();
   private Properties properties;

   public PropertiesConfiguration(final LoggerContext loggerContext, final ConfigurationSource source, final int monitorIntervalSeconds) {
      super(loggerContext, source, monitorIntervalSeconds);
   }

   public PropertiesConfiguration(final LoggerContext loggerContext, final Properties properties) {
      super(loggerContext, ConfigurationSource.NULL_SOURCE, 0);
      this.properties = properties;
   }

   public PropertiesConfiguration(org.apache.logging.log4j.spi.LoggerContext loggerContext, Properties properties) {
      this((LoggerContext)loggerContext, properties);
   }

   public void doConfigure() {
      if (this.properties == null) {
         this.properties = new Properties();
         InputStream inputStream = this.getConfigurationSource().getInputStream();
         if (inputStream != null) {
            try {
               this.properties.load(inputStream);
            } catch (Exception e) {
               LOGGER.error("Could not read configuration file [{}].", this.getConfigurationSource().toString(), e);
               return;
            }
         }
      }

      this.doConfigure(this.properties);
   }

   @Override
   public Configuration reconfigure() {
      try {
         ConfigurationSource source = this.getConfigurationSource().resetInputStream();
         if (source == null) {
            return null;
         }

         Configuration config = new PropertiesConfigurationFactory().getConfiguration(this.getLoggerContext(), source);
         return config != null && config.getState() == State.INITIALIZING ? config : null;
      } catch (IOException ex) {
         LOGGER.error("Cannot locate file {}: {}", this.getConfigurationSource(), ex);
         return null;
      }
   }

   private void doConfigure(final Properties properties) {
      String status = "error";
      String value = properties.getProperty("log4j.debug");
      if (value == null) {
         value = properties.getProperty("log4j.configDebug");
         if (value != null) {
            LOGGER.warn("[log4j.configDebug] is deprecated. Use [log4j.debug] instead.");
         }
      }

      if (value != null) {
         status = OptionConverter.toBoolean(value, false) ? "debug" : "error";
      }

      StatusConfiguration statusConfig = new StatusConfiguration().setStatus(status);
      statusConfig.initialize();
      String reset = properties.getProperty("log4j.reset");
      if (reset != null && OptionConverter.toBoolean(reset, false)) {
         LogManager.resetConfiguration();
      }

      String threshold = OptionConverter.findAndSubst("log4j.threshold", properties);
      if (threshold != null) {
         Level level = OptionConverter.convertLevel(threshold.trim(), Level.ALL);
         this.addFilter(ThresholdFilter.createFilter(level, Result.NEUTRAL, Result.DENY));
      }

      this.configureRoot(properties);
      this.parseLoggers(properties);
      LOGGER.debug("Finished configuring.");
   }

   private void configureRoot(final Properties props) {
      String effectiveFrefix = "log4j.rootLogger";
      String value = OptionConverter.findAndSubst("log4j.rootLogger", props);
      if (value == null) {
         value = OptionConverter.findAndSubst("log4j.rootCategory", props);
         effectiveFrefix = "log4j.rootCategory";
      }

      if (value == null) {
         LOGGER.debug("Could not find root logger information. Is this OK?");
      } else {
         LoggerConfig root = this.getRootLogger();
         this.parseLogger(props, root, effectiveFrefix, "root", value);
      }
   }

   private void parseLoggers(final Properties props) {
      Enumeration<?> enumeration = props.propertyNames();

      while (enumeration.hasMoreElements()) {
         String key = Objects.toString(enumeration.nextElement(), null);
         if (key.startsWith("log4j.category.") || key.startsWith("log4j.logger.")) {
            String loggerName = null;
            if (key.startsWith("log4j.category.")) {
               loggerName = key.substring("log4j.category.".length());
            } else if (key.startsWith("log4j.logger.")) {
               loggerName = key.substring("log4j.logger.".length());
            }

            String value = OptionConverter.findAndSubst(key, props);
            LoggerConfig loggerConfig = this.getLogger(loggerName);
            if (loggerConfig == null) {
               boolean additivity = this.getAdditivityForLogger(props, loggerName);
               loggerConfig = new LoggerConfig(loggerName, Level.ERROR, additivity);
               this.addLogger(loggerName, loggerConfig);
            }

            this.parseLogger(props, loggerConfig, key, loggerName, value);
         }
      }
   }

   private boolean getAdditivityForLogger(final Properties props, final String loggerName) {
      boolean additivity = true;
      String key = "log4j.additivity." + loggerName;
      String value = OptionConverter.findAndSubst(key, props);
      LOGGER.debug("Handling {}=[{}]", key, value);
      if (value != null && !value.equals("")) {
         additivity = OptionConverter.toBoolean(value, true);
      }

      return additivity;
   }

   private void parseLogger(final Properties props, final LoggerConfig loggerConfig, final String optionKey, final String loggerName, final String value) {
      LOGGER.debug("Parsing for [{}] with value=[{}].", loggerName, value);
      StringTokenizer st = new StringTokenizer(value, ",");
      if (!value.startsWith(",") && !value.equals("")) {
         if (!st.hasMoreTokens()) {
            return;
         }

         String levelStr = st.nextToken();
         LOGGER.debug("Level token is [{}].", levelStr);
         Level level = levelStr == null ? Level.ERROR : OptionConverter.convertLevel(levelStr, Level.DEBUG);
         loggerConfig.setLevel(level);
         LOGGER.debug("Logger {} level set to {}", loggerName, level);
      }

      while (st.hasMoreTokens()) {
         String appenderName = st.nextToken().trim();
         if (appenderName != null && !appenderName.equals(",")) {
            LOGGER.debug("Parsing appender named \"{}\".", appenderName);
            Appender appender = this.parseAppender(props, appenderName);
            if (appender != null) {
               LOGGER.debug("Adding appender named [{}] to loggerConfig [{}].", appenderName, loggerConfig.getName());
               loggerConfig.addAppender(this.getAppender(appenderName), null, null);
            } else {
               LOGGER.debug("Appender named [{}] not found.", appenderName);
            }
         }
      }
   }

   public Appender parseAppender(final Properties props, final String appenderName) {
      Appender appender = this.registry.get(appenderName);
      if (appender != null) {
         LOGGER.debug("Appender \"" + appenderName + "\" was already parsed.");
         return appender;
      }

      String prefix = "log4j.appender." + appenderName;
      String layoutPrefix = prefix + ".layout";
      String filterPrefix = "log4j.appender." + appenderName + ".filter.";
      String className = OptionConverter.findAndSubst(prefix, props);
      if (className == null) {
         LOGGER.debug("Appender \"" + appenderName + "\" does not exist.");
         return null;
      }

      appender = this.manager.parseAppender(appenderName, className, prefix, layoutPrefix, filterPrefix, props, this);
      if (appender == null) {
         appender = this.buildAppender(appenderName, className, prefix, layoutPrefix, filterPrefix, props);
      } else {
         this.registry.put(appenderName, appender);
         this.addAppender(AppenderAdapter.adapt(appender));
      }

      return appender;
   }

   private Appender buildAppender(
      final String appenderName, final String className, final String prefix, final String layoutPrefix, final String filterPrefix, final Properties props
   ) {
      Appender appender = newInstanceOf(className, "Appender");
      if (appender == null) {
         return null;
      }

      appender.setName(appenderName);
      appender.setLayout(this.parseLayout(layoutPrefix, appenderName, props));
      String errorHandlerPrefix = prefix + ".errorhandler";
      String errorHandlerClass = OptionConverter.findAndSubst(errorHandlerPrefix, props);
      if (errorHandlerClass != null) {
         ErrorHandler eh = this.parseErrorHandler(props, errorHandlerPrefix, errorHandlerClass, appender);
         if (eh != null) {
            appender.setErrorHandler(eh);
         }
      }

      appender.addFilter(this.parseAppenderFilters(props, filterPrefix, appenderName));
      String[] keys = new String[]{layoutPrefix};
      this.addProperties(appender, keys, props, prefix);
      this.addAppender(AppenderAdapter.adapt(appender));
      this.registry.put(appenderName, appender);
      return appender;
   }

   public Layout parseLayout(final String layoutPrefix, final String appenderName, final Properties props) {
      String layoutClass = OptionConverter.findAndSubst(layoutPrefix, props);
      if (layoutClass == null) {
         return null;
      }

      Layout layout = this.manager.parse(layoutClass, layoutPrefix, props, this, BuilderManager.INVALID_LAYOUT);
      if (layout == null) {
         layout = this.buildLayout(layoutPrefix, layoutClass, appenderName, props);
      }

      return layout;
   }

   private Layout buildLayout(final String layoutPrefix, final String className, final String appenderName, final Properties props) {
      Layout layout = newInstanceOf(className, "Layout");
      if (layout == null) {
         return null;
      }

      LOGGER.debug("Parsing layout options for \"{}\".", appenderName);
      PropertySetter.setProperties(layout, props, layoutPrefix + ".");
      LOGGER.debug("End of parsing for \"{}\".", appenderName);
      return layout;
   }

   public ErrorHandler parseErrorHandler(final Properties props, final String errorHandlerPrefix, final String errorHandlerClass, final Appender appender) {
      ErrorHandler eh = newInstanceOf(errorHandlerClass, "ErrorHandler");
      String[] keys = new String[]{errorHandlerPrefix + ".root-ref", errorHandlerPrefix + ".logger-ref", errorHandlerPrefix + ".appender-ref"};
      this.addProperties(eh, keys, props, errorHandlerPrefix);
      return eh;
   }

   public void addProperties(final Object obj, final String[] keys, final Properties props, final String prefix) {
      Properties edited = new Properties();
      props.stringPropertyNames().stream().filter(name -> {
         if (name.startsWith(prefix)) {
            for (String key : keys) {
               if (name.equals(key)) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }).forEach(name -> edited.put(name, props.getProperty(name)));
      PropertySetter.setProperties(obj, edited, prefix + ".");
   }

   public Filter parseAppenderFilters(final Properties props, final String filterPrefix, final String appenderName) {
      int fIdx = filterPrefix.length();
      SortedMap<String, List<PropertiesConfiguration.NameValue>> filters = new TreeMap<>();
      Enumeration<?> e = props.keys();
      String name = "";

      while (e.hasMoreElements()) {
         String key = (String)e.nextElement();
         if (key.startsWith(filterPrefix)) {
            int dotIdx = key.indexOf(46, fIdx);
            String filterKey = key;
            if (dotIdx != -1) {
               filterKey = key.substring(0, dotIdx);
               name = key.substring(dotIdx + 1);
            }

            List<PropertiesConfiguration.NameValue> filterOpts = filters.computeIfAbsent(filterKey, k -> new ArrayList<>());
            if (dotIdx != -1) {
               String value = OptionConverter.findAndSubst(key, props);
               filterOpts.add(new PropertiesConfiguration.NameValue(name, value));
            }
         }
      }

      Filter head = null;

      for (Entry<String, List<PropertiesConfiguration.NameValue>> entry : filters.entrySet()) {
         String clazz = props.getProperty(entry.getKey());
         Filter filter = null;
         if (clazz != null) {
            filter = this.manager.parse(clazz, entry.getKey(), props, this, BuilderManager.INVALID_FILTER);
            if (filter == null) {
               LOGGER.debug("Filter key: [{}] class: [{}] props: {}", entry.getKey(), clazz, entry.getValue());
               filter = this.buildFilter(clazz, appenderName, entry.getValue());
            }
         }

         head = FilterAdapter.addFilter(head, filter);
      }

      return head;
   }

   private Filter buildFilter(final String className, final String appenderName, final List<PropertiesConfiguration.NameValue> props) {
      Filter filter = newInstanceOf(className, "Filter");
      if (filter != null) {
         PropertySetter propSetter = new PropertySetter(filter);

         for (PropertiesConfiguration.NameValue property : props) {
            propSetter.setProperty(property.key, property.value);
         }

         propSetter.activate();
      }

      return filter;
   }

   private static <T> T newInstanceOf(final String className, final String type) {
      try {
         return (T)LoaderUtil.newInstanceOf(className);
      } catch (ReflectiveOperationException ex) {
         LOGGER.error("Unable to create {} {} due to {}:{}", type, className, ex.getClass().getSimpleName(), ex.getMessage(), ex);
         return null;
      }
   }

   private static class NameValue {
      String key;
      String value;

      NameValue(final String key, final String value) {
         this.key = key;
         this.value = value;
      }

      @Override
      public String toString() {
         return this.key + "=" + this.value;
      }
   }
}
