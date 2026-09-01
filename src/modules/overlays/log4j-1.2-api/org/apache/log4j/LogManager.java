package org.apache.log4j;

import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;
import org.apache.log4j.legacy.core.ContextUtil;
import org.apache.log4j.spi.DefaultRepositorySelector;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.NOPLoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.StackLocatorUtil;

public final class LogManager {
   @Deprecated
   public static final String DEFAULT_CONFIGURATION_FILE = "log4j.properties";
   @Deprecated
   public static final String DEFAULT_CONFIGURATION_KEY = "log4j.configuration";
   @Deprecated
   public static final String CONFIGURATOR_CLASS_KEY = "log4j.configuratorClass";
   @Deprecated
   public static final String DEFAULT_INIT_OVERRIDE_KEY = "log4j.defaultInitOverride";
   static final String DEFAULT_XML_CONFIGURATION_FILE = "log4j.xml";
   private static RepositorySelector repositorySelector;
   private static final boolean LOG4J_CORE_PRESENT = LoaderUtil.isClassAvailable("org.apache.logging.log4j.core.LoggerContext");

   public static Logger exists(final String name) {
      return exists(name, StackLocatorUtil.getCallerClassLoader(2));
   }

   static Logger exists(final String name, final ClassLoader classLoader) {
      return getHierarchy().exists(name, classLoader);
   }

   static LoggerContext getContext(final ClassLoader classLoader) {
      return org.apache.logging.log4j.LogManager.getContext(classLoader, false);
   }

   public static Enumeration getCurrentLoggers() {
      return getCurrentLoggers(StackLocatorUtil.getCallerClassLoader(2));
   }

   static Enumeration getCurrentLoggers(final ClassLoader classLoader) {
      return Collections.enumeration(
         getContext(classLoader).getLoggerRegistry().getLoggers().stream().map(e -> getLogger(e.getName(), classLoader)).collect(Collectors.toList())
      );
   }

   static Hierarchy getHierarchy() {
      LoggerRepository loggerRepository = getLoggerRepository();
      return loggerRepository instanceof Hierarchy ? (Hierarchy)loggerRepository : null;
   }

   public static Logger getLogger(final Class<?> clazz) {
      Hierarchy hierarchy = getHierarchy();
      return hierarchy != null
         ? hierarchy.getLogger(clazz.getName(), StackLocatorUtil.getCallerClassLoader(2))
         : getLoggerRepository().getLogger(clazz.getName());
   }

   public static Logger getLogger(final String name) {
      Hierarchy hierarchy = getHierarchy();
      return hierarchy != null ? hierarchy.getLogger(name, StackLocatorUtil.getCallerClassLoader(2)) : getLoggerRepository().getLogger(name);
   }

   static Logger getLogger(final String name, final ClassLoader classLoader) {
      Hierarchy hierarchy = getHierarchy();
      return hierarchy != null ? hierarchy.getLogger(name, classLoader) : getLoggerRepository().getLogger(name);
   }

   public static Logger getLogger(final String name, final LoggerFactory factory) {
      Hierarchy hierarchy = getHierarchy();
      return hierarchy != null ? hierarchy.getLogger(name, factory, StackLocatorUtil.getCallerClassLoader(2)) : getLoggerRepository().getLogger(name, factory);
   }

   static Logger getLogger(final String name, final LoggerFactory factory, final ClassLoader classLoader) {
      Hierarchy hierarchy = getHierarchy();
      return hierarchy != null ? hierarchy.getLogger(name, factory, classLoader) : getLoggerRepository().getLogger(name, factory);
   }

   public static LoggerRepository getLoggerRepository() {
      if (repositorySelector == null) {
         repositorySelector = new DefaultRepositorySelector(new NOPLoggerRepository());
      }

      return repositorySelector.getLoggerRepository();
   }

   public static Logger getRootLogger() {
      return getRootLogger(StackLocatorUtil.getCallerClassLoader(2));
   }

   static Logger getRootLogger(final ClassLoader classLoader) {
      Hierarchy hierarchy = getHierarchy();
      return hierarchy != null ? hierarchy.getRootLogger(classLoader) : getLoggerRepository().getRootLogger();
   }

   static boolean isLog4jCorePresent() {
      return LOG4J_CORE_PRESENT;
   }

   static void reconfigure(final ClassLoader classLoader) {
      if (isLog4jCorePresent()) {
         ContextUtil.reconfigure(getContext(classLoader));
      }
   }

   public static void resetConfiguration() {
      resetConfiguration(StackLocatorUtil.getCallerClassLoader(2));
   }

   static void resetConfiguration(final ClassLoader classLoader) {
      Hierarchy hierarchy = getHierarchy();
      if (hierarchy != null) {
         hierarchy.resetConfiguration(classLoader);
      } else {
         getLoggerRepository().resetConfiguration();
      }
   }

   public static void setRepositorySelector(final RepositorySelector selector, final Object guard) throws IllegalArgumentException {
      if (selector == null) {
         throw new IllegalArgumentException("RepositorySelector must be non-null.");
      }

      repositorySelector = selector;
   }

   public static void shutdown() {
      shutdown(StackLocatorUtil.getCallerClassLoader(2));
   }

   static void shutdown(final ClassLoader classLoader) {
      Hierarchy hierarchy = getHierarchy();
      if (hierarchy != null) {
         hierarchy.shutdown(classLoader);
      } else {
         getLoggerRepository().shutdown();
      }
   }

   static {
      Hierarchy hierarchy = new Hierarchy(new RootLogger(Level.DEBUG));
      repositorySelector = new DefaultRepositorySelector(hierarchy);
   }
}
