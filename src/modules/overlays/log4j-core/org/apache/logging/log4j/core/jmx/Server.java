package org.apache.logging.log4j.core.jmx;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.PropertiesUtil;

public final class Server {
   private static final String CONTEXT_NAME_ALL = "*";
   public static final String DOMAIN = "org.apache.logging.log4j2";
   private static final String THREAD_NAME_PREFIX = "jmx.notif";
   private static final StatusLogger LOGGER = StatusLogger.getLogger();
   static final Executor executor = isJmxDisabled() ? null : createExecutor();

   private Server() {
   }

   private static ExecutorService createExecutor() {
      boolean defaultAsync = !Constants.isWebApp();
      boolean async = PropertiesUtil.getProperties().getBooleanProperty(Log4jPropertyKey.JMX_NOTIFY_ASYNC, defaultAsync);
      return async ? Executors.newFixedThreadPool(1, Log4jThreadFactory.createDaemonThreadFactory("jmx.notif")) : null;
   }

   public static String escape(final String name) {
      StringBuilder sb = new StringBuilder(name.length() * 2);
      boolean needsQuotes = false;

      for (int i = 0; i < name.length(); i++) {
         char c = name.charAt(i);
         switch (c) {
            case '\n':
               sb.append("\\n");
               needsQuotes = true;
            case '\r':
               continue;
            case '"':
            case '*':
            case '?':
            case '\\':
               sb.append('\\');
               needsQuotes = true;
               break;
            case ',':
            case ':':
            case '=':
               needsQuotes = true;
         }

         sb.append(c);
      }

      if (needsQuotes) {
         sb.insert(0, '"');
         sb.append('"');
      }

      return sb.toString();
   }

   private static boolean isJmxDisabled() {
      return !PropertiesUtil.getProperties().getBooleanProperty(Log4jPropertyKey.JMX_ENABLED, true);
   }

   public static void reregisterMBeansAfterReconfigure() {
      if (isJmxDisabled()) {
         LOGGER.debug("JMX disabled for Log4j2. Not registering MBeans.");
      } else {
         reregisterMBeansAfterReconfigure(ManagementFactory.getPlatformMBeanServer());
      }
   }

   public static void reregisterMBeansAfterReconfigure(final MBeanServer mbs) {
      if (isJmxDisabled()) {
         LOGGER.debug("JMX disabled for Log4j2. Not registering MBeans.");
      } else {
         try {
            ContextSelector selector = getContextSelector();
            if (selector == null) {
               LOGGER.debug("Could not register MBeans: no ContextSelector found.");
               return;
            }

            LOGGER.trace("Reregistering MBeans after reconfigure. Selector={}", selector);
            List<LoggerContext> contexts = selector.getLoggerContexts();
            int i = 0;

            for (LoggerContext ctx : contexts) {
               LOGGER.trace("Reregistering context ({}/{}): '{}' {}", ++i, contexts.size(), ctx.getName(), ctx);
               unregisterLoggerContext(ctx.getName(), mbs);
               LoggerContextAdmin mbean = new LoggerContextAdmin(ctx, executor);
               register(mbs, mbean, mbean.getObjectName());
               if (ctx instanceof AsyncLoggerContext) {
                  RingBufferAdmin rbmbean = ((AsyncLoggerContext)ctx).createRingBufferAdmin();
                  if (rbmbean.getBufferSize() > 0L) {
                     register(mbs, rbmbean, rbmbean.getObjectName());
                  }
               }

               registerStatusLogger(ctx.getName(), mbs, executor);
               registerContextSelector(ctx.getName(), selector, mbs, executor);
               registerLoggerConfigs(ctx, mbs, executor);
               registerAppenders(ctx, mbs, executor);
            }
         } catch (Exception ex) {
            LOGGER.error("Could not register mbeans", ex);
         }
      }
   }

   public static void unregisterMBeans() {
      if (isJmxDisabled()) {
         LOGGER.debug("JMX disabled for Log4j2. Not unregistering MBeans.");
      } else {
         unregisterMBeans(ManagementFactory.getPlatformMBeanServer());
      }
   }

   public static void unregisterMBeans(final MBeanServer mbs) {
      if (mbs != null) {
         unregisterStatusLogger("*", mbs);
         unregisterContextSelector("*", mbs);
         unregisterContexts(mbs);
         unregisterLoggerConfigs("*", mbs);
         unregisterAsyncLoggerRingBufferAdmins("*", mbs);
         unregisterAsyncLoggerConfigRingBufferAdmins("*", mbs);
         unregisterAppenders("*", mbs);
         unregisterAsyncAppenders("*", mbs);
      }
   }

   private static ContextSelector getContextSelector() {
      LoggerContextFactory factory = LogManager.getFactory();
      return factory instanceof Log4jContextFactory ? ((Log4jContextFactory)factory).getSelector() : null;
   }

   public static void unregisterLoggerContext(final String loggerContextName) {
      if (isJmxDisabled()) {
         LOGGER.debug("JMX disabled for Log4j2. Not unregistering MBeans.");
      } else {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         unregisterLoggerContext(loggerContextName, mbs);
      }
   }

   public static void unregisterLoggerContext(final String contextName, final MBeanServer mbs) {
      String search = String.format("org.apache.logging.log4j2:type=%s", escape(contextName));
      unregisterAllMatching(search, mbs);
      unregisterStatusLogger(contextName, mbs);
      unregisterContextSelector(contextName, mbs);
      unregisterLoggerConfigs(contextName, mbs);
      unregisterAppenders(contextName, mbs);
      unregisterAsyncAppenders(contextName, mbs);
      unregisterAsyncLoggerRingBufferAdmins(contextName, mbs);
      unregisterAsyncLoggerConfigRingBufferAdmins(contextName, mbs);
   }

   private static void registerStatusLogger(final String contextName, final MBeanServer mbs, final Executor executor) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
      StatusLoggerAdmin mbean = new StatusLoggerAdmin(contextName, executor);
      register(mbs, mbean, mbean.getObjectName());
   }

   private static void registerContextSelector(final String contextName, final ContextSelector selector, final MBeanServer mbs, final Executor executor) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
      ContextSelectorAdmin mbean = new ContextSelectorAdmin(contextName, selector);
      register(mbs, mbean, mbean.getObjectName());
   }

   private static void unregisterStatusLogger(final String contextName, final MBeanServer mbs) {
      String search = String.format("org.apache.logging.log4j2:type=%s,component=StatusLogger", escape(contextName));
      unregisterAllMatching(search, mbs);
   }

   private static void unregisterContextSelector(final String contextName, final MBeanServer mbs) {
      String search = String.format("org.apache.logging.log4j2:type=%s,component=ContextSelector", escape(contextName));
      unregisterAllMatching(search, mbs);
   }

   private static void unregisterLoggerConfigs(final String contextName, final MBeanServer mbs) {
      String pattern = "org.apache.logging.log4j2:type=%s,component=Loggers,name=%s";
      String search = String.format("org.apache.logging.log4j2:type=%s,component=Loggers,name=%s", escape(contextName), "*");
      unregisterAllMatching(search, mbs);
   }

   private static void unregisterContexts(final MBeanServer mbs) {
      String pattern = "org.apache.logging.log4j2:type=%s";
      String search = String.format("org.apache.logging.log4j2:type=%s", "*");
      unregisterAllMatching(search, mbs);
   }

   private static void unregisterAppenders(final String contextName, final MBeanServer mbs) {
      String pattern = "org.apache.logging.log4j2:type=%s,component=Appenders,name=%s";
      String search = String.format("org.apache.logging.log4j2:type=%s,component=Appenders,name=%s", escape(contextName), "*");
      unregisterAllMatching(search, mbs);
   }

   private static void unregisterAsyncAppenders(final String contextName, final MBeanServer mbs) {
      String pattern = "org.apache.logging.log4j2:type=%s,component=AsyncAppenders,name=%s";
      String search = String.format("org.apache.logging.log4j2:type=%s,component=AsyncAppenders,name=%s", escape(contextName), "*");
      unregisterAllMatching(search, mbs);
   }

   private static void unregisterAsyncLoggerRingBufferAdmins(final String contextName, final MBeanServer mbs) {
      String pattern1 = "org.apache.logging.log4j2:type=%s,component=AsyncLoggerRingBuffer";
      String search1 = String.format("org.apache.logging.log4j2:type=%s,component=AsyncLoggerRingBuffer", escape(contextName));
      unregisterAllMatching(search1, mbs);
   }

   private static void unregisterAsyncLoggerConfigRingBufferAdmins(final String contextName, final MBeanServer mbs) {
      String pattern2 = "org.apache.logging.log4j2:type=%s,component=Loggers,name=%s,subtype=RingBuffer";
      String search2 = String.format("org.apache.logging.log4j2:type=%s,component=Loggers,name=%s,subtype=RingBuffer", escape(contextName), "*");
      unregisterAllMatching(search2, mbs);
   }

   private static void unregisterAllMatching(final String search, final MBeanServer mbs) {
      try {
         ObjectName pattern = new ObjectName(search);
         Set<ObjectName> found = mbs.queryNames(pattern, null);
         if (found != null && !found.isEmpty()) {
            LOGGER.trace("Unregistering {} MBeans: {}", found.size(), found);
         } else {
            LOGGER.trace("Unregistering but no MBeans found matching '{}'", search);
         }

         if (found != null) {
            for (ObjectName objectName : found) {
               mbs.unregisterMBean(objectName);
            }
         }
      } catch (InstanceNotFoundException ex) {
         LOGGER.debug("Could not unregister MBeans for " + search + ". Ignoring " + ex);
      } catch (Exception ex) {
         LOGGER.error("Could not unregister MBeans for " + search, ex);
      }
   }

   private static void registerLoggerConfigs(final LoggerContext ctx, final MBeanServer mbs, final Executor executor) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
      Map<String, LoggerConfig> map = ctx.getConfiguration().getLoggers();

      for (String name : map.keySet()) {
         LoggerConfig cfg = map.get(name);
         LoggerConfigAdmin mbean = new LoggerConfigAdmin(ctx, cfg);
         register(mbs, mbean, mbean.getObjectName());
         if (cfg instanceof AsyncLoggerConfig) {
            AsyncLoggerConfig async = (AsyncLoggerConfig)cfg;
            RingBufferAdmin rbmbean = async.createRingBufferAdmin(ctx.getName());
            register(mbs, rbmbean, rbmbean.getObjectName());
         }
      }
   }

   private static void registerAppenders(final LoggerContext ctx, final MBeanServer mbs, final Executor executor) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
      Map<String, Appender> map = ctx.getConfiguration().getAppenders();

      for (String name : map.keySet()) {
         Appender appender = map.get(name);
         if (appender instanceof AsyncAppender) {
            AsyncAppender async = (AsyncAppender)appender;
            AsyncAppenderAdmin mbean = new AsyncAppenderAdmin(ctx.getName(), async);
            register(mbs, mbean, mbean.getObjectName());
         } else {
            AppenderAdmin mbean = new AppenderAdmin(ctx.getName(), appender);
            register(mbs, mbean, mbean.getObjectName());
         }
      }
   }

   private static void register(final MBeanServer mbs, final Object mbean, final ObjectName objectName) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
      if (mbs.isRegistered(objectName)) {
         try {
            mbs.unregisterMBean(objectName);
         } catch (MBeanRegistrationException | InstanceNotFoundException ex) {
            LOGGER.trace("Failed to unregister MBean {}", objectName);
         }
      }

      LOGGER.debug("Registering MBean {}", objectName);
      mbs.registerMBean(mbean, objectName);
   }
}
