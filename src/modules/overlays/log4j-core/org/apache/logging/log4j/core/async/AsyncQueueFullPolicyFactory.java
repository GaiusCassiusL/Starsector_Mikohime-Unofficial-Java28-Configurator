package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;

public class AsyncQueueFullPolicyFactory {
   static final String PROPERTY_VALUE_DEFAULT_ASYNC_EVENT_ROUTER = "Default";
   static final String PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER = "Discard";
   private static final Logger LOGGER = StatusLogger.getLogger();

   public static AsyncQueueFullPolicy create() {
      String router = PropertiesUtil.getProperties().getStringProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY);
      if (router == null || isRouterSelected(router, DefaultAsyncQueueFullPolicy.class, "Default")) {
         return new DefaultAsyncQueueFullPolicy();
      } else {
         return isRouterSelected(router, DiscardingAsyncQueueFullPolicy.class, "Discard") ? createDiscardingAsyncQueueFullPolicy() : createCustomRouter(router);
      }
   }

   private static boolean isRouterSelected(final String propertyValue, final Class<? extends AsyncQueueFullPolicy> policy, final String shortPropertyValue) {
      return propertyValue != null
         && (shortPropertyValue.equalsIgnoreCase(propertyValue) || policy.getName().equals(propertyValue) || policy.getSimpleName().equals(propertyValue));
   }

   private static AsyncQueueFullPolicy createCustomRouter(final String router) {
      try {
         Class<? extends AsyncQueueFullPolicy> cls = Loader.loadClass(router).asSubclass(AsyncQueueFullPolicy.class);
         LOGGER.debug("Creating custom AsyncQueueFullPolicy '{}'", router);
         return cls.newInstance();
      } catch (Exception ex) {
         LOGGER.debug("Using DefaultAsyncQueueFullPolicy. Could not create custom AsyncQueueFullPolicy '{}': {}", router, ex.toString());
         return new DefaultAsyncQueueFullPolicy();
      }
   }

   private static AsyncQueueFullPolicy createDiscardingAsyncQueueFullPolicy() {
      PropertyEnvironment properties = PropertiesUtil.getProperties();
      String level = properties.getStringProperty(Log4jPropertyKey.ASYNC_LOGGER_DISCARD_THRESHOLD, Level.INFO.name());
      Level thresholdLevel = Level.toLevel(level, Level.INFO);
      LOGGER.debug("Creating custom DiscardingAsyncQueueFullPolicy(discardThreshold:{})", thresholdLevel);
      return new DiscardingAsyncQueueFullPolicy(thresholdLevel);
   }
}
