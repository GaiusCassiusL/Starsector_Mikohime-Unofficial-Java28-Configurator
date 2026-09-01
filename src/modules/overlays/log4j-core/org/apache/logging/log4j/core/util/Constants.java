package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyKey;

public final class Constants {
   private static final String JNDI_MANAGER_CLASS = "org.apache.logging.log4j.jndi.JndiManager";
   public static boolean JNDI_CONTEXT_SELECTOR_ENABLED = isJndiEnabled(Log4jPropertyKey.JNDI_CONTEXT_SELECTOR);
   public static boolean JNDI_JMS_ENABLED = isJndiEnabled(Log4jPropertyKey.JNDI_ENABLE_JMS);
   public static boolean JNDI_LOOKUP_ENABLED = isJndiEnabled(Log4jPropertyKey.JNDI_ENABLE_LOOKUP);
   public static boolean JNDI_JDBC_ENABLED = isJndiEnabled(Log4jPropertyKey.JNDI_ENABLE_JDBC);
   public static final Key<Level> DEFAULT_STATUS_LEVEL_KEY = new Key<Level>() {};
   public static final String JNDI_CONTEXT_NAME = "java:comp/env/log4j/context-name";
   public static final int MILLIS_IN_SECONDS = 1000;
   public static final boolean FORMAT_MESSAGES_IN_BACKGROUND = PropertiesUtil.getProperties()
      .getBooleanProperty(Log4jPropertyKey.ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND, false);
   public static final boolean ENABLE_DIRECT_ENCODERS = PropertiesUtil.getProperties().getBooleanProperty(Log4jPropertyKey.GC_ENABLE_DIRECT_ENCODERS, true);
   public static final int INITIAL_REUSABLE_MESSAGE_SIZE = size(Log4jPropertyKey.GC_INITIAL_REUSABLE_MESSAGE_SIZE, 128);
   public static final int MAX_REUSABLE_MESSAGE_SIZE = size(Log4jPropertyKey.GC_REUSABLE_MESSAGE_MAX_SIZE, 518);
   public static final int ENCODER_CHAR_BUFFER_SIZE = size(Log4jPropertyKey.GC_ENCODER_CHAR_BUFFER_SIZE, 2048);
   public static final int ENCODER_BYTE_BUFFER_SIZE = size(Log4jPropertyKey.GC_ENCODER_BYTE_BUFFER_SIZE, 8192);

   private static boolean isJndiEnabled(final PropertyKey key) {
      return PropertiesUtil.getProperties().getBooleanProperty(key, false) && isClassAvailable("org.apache.logging.log4j.jndi.JndiManager");
   }

   private static int size(final PropertyKey property, final int defaultValue) {
      return PropertiesUtil.getProperties().getIntegerProperty(property, defaultValue);
   }

   private static boolean isClassAvailable(final String className) {
      try {
         return LoaderUtil.loadClass(className) != null;
      } catch (Throwable e) {
         return false;
      }
   }

   private Constants() {
   }
}
