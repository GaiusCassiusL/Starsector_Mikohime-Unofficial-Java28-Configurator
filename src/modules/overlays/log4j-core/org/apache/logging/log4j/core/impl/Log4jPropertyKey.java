package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.spi.PropertyComponent;
import org.apache.logging.log4j.util.PropertyKey;

public enum Log4jPropertyKey implements PropertyKey {
   ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME(PropertyComponent.ASYNC_LOGGER_CONFIG, "exceptionHandler"),
   ASYNC_CONFIG_RETRIES(PropertyComponent.ASYNC_LOGGER_CONFIG, "retries"),
   ASYNC_CONFIG_RING_BUFFER_SIZE(PropertyComponent.ASYNC_LOGGER_CONFIG, "ringBufferSize"),
   ASYNC_CONFIG_SLEEP_TIME_NS(PropertyComponent.ASYNC_LOGGER_CONFIG, "sleepTimeNS"),
   ASYNC_CONFIG_TIMEOUT(PropertyComponent.ASYNC_LOGGER_CONFIG, "timeout"),
   ASYNC_CONFIG_WAIT_STRATEGY(PropertyComponent.ASYNC_LOGGER_CONFIG, "waitStrategy"),
   ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL(PropertyComponent.ASYNC_LOGGER_CONFIG, "synchronizeEnqueueWhenQueueFull"),
   ASYNC_LOGGER_DISCARD_THRESHOLD(PropertyComponent.ASYNC_LOGGER, "discardThreshold"),
   ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME(PropertyComponent.ASYNC_LOGGER, "exceptionHandler"),
   ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND(PropertyComponent.ASYNC_LOGGER, "formatMsg"),
   ASYNC_LOGGER_QUEUE_FULL_POLICY(PropertyComponent.ASYNC_LOGGER, "queueFullPolicy"),
   ASYNC_LOGGER_RETRIES(PropertyComponent.ASYNC_LOGGER, "retries"),
   ASYNC_LOGGER_RING_BUFFER_SIZE(PropertyComponent.ASYNC_LOGGER, "ringBufferSize"),
   ASYNC_LOGGER_SLEEP_TIME_NS(PropertyComponent.ASYNC_LOGGER, "sleepTimeNS"),
   ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL(PropertyComponent.ASYNC_LOGGER, "synchronizeEnqueueWhenQueueFull"),
   ASYNC_LOGGER_THREAD_NAME_STRATEGY(PropertyComponent.ASYNC_LOGGER, "threadNameStrategy"),
   ASYNC_LOGGER_TIMEOUT(PropertyComponent.ASYNC_LOGGER, "timeout"),
   ASYNC_LOGGER_WAIT_STRATEGY(PropertyComponent.ASYNC_LOGGER, "waitStrategy"),
   CONFIG_AUTH_PROVIDER(PropertyComponent.CONFIGURATION, "authorizationProvider"),
   CONFIG_CLOCK(PropertyComponent.CONFIGURATION, "clock"),
   CONFIG_CONFIGURATION_FACTORY_CLASS_NAME(PropertyComponent.CONFIGURATION, "configurationFactory"),
   CONFIG_DEFAULT_LEVEL(PropertyComponent.CONFIGURATION, "level"),
   CONFIG_LOCATION(PropertyComponent.CONFIGURATION, "file"),
   CONSOLE_JANSI_ENABLED(PropertyComponent.CONSOLE, "jansiEnabled"),
   CONFIG_MERGE_STRATEGY(PropertyComponent.CONFIGURATION, "mergeStrategy"),
   CONFIG_RELIABILITY_STRATEGY(PropertyComponent.CONFIGURATION, "reliabilityStrategy"),
   CONFIG_RELIABILITY_STRATEGY_AWAIT_UNCONDITIONALLY_MILLIS(PropertyComponent.CONFIGURATION, "waitMillisBeforeStopOldConfig"),
   CONFIG_V1_COMPATIBILITY_ENABLED(PropertyComponent.LOG4J1, "compatibility"),
   CONFIG_V1_FILE_NAME(PropertyComponent.LOG4J, "configuration"),
   CONTEXT_SELECTOR_CLASS_NAME(PropertyComponent.LOGGER_CONTEXT, "selector"),
   GC_ENABLE_DIRECT_ENCODERS(PropertyComponent.GC, "enableDirectEncoders"),
   GC_ENCODER_BYTE_BUFFER_SIZE(PropertyComponent.GC, "encoderByteBufferSize"),
   GC_ENCODER_CHAR_BUFFER_SIZE(PropertyComponent.GC, "encoderCharBufferSize"),
   GC_INITIAL_REUSABLE_MESSAGE_SIZE(PropertyComponent.GC, "initialReusableMsgSize"),
   GC_LAYOUT_STRING_BUILDER_MAX_SIZE(PropertyComponent.GC, "layoutStringBuilderMaxSize"),
   GC_REUSABLE_MESSAGE_MAX_SIZE(PropertyComponent.GC, "maxReusableMsgSize"),
   JMX_ENABLED(PropertyComponent.JMX, "enabled"),
   JMX_NOTIFY_ASYNC(PropertyComponent.JMX, "notifyAsync"),
   JNDI_CONTEXT_SELECTOR(PropertyComponent.JNDI, "contextSelector"),
   JNDI_ENABLE_JDBC(PropertyComponent.JNDI, "enableJDBC"),
   JNDI_ENABLE_JMS(PropertyComponent.JNDI, "enableJMS"),
   JNDI_ENABLE_LOOKUP(PropertyComponent.JNDI, "enableLookup"),
   LOG_EVENT_FACTORY_CLASS_NAME(PropertyComponent.LOGGER, "logEventFactory"),
   SHUTDOWN_CALLBACK_REGISTRY(PropertyComponent.LOGGER_CONTEXT, "shutdownCallbackRegistry"),
   SHUTDOWN_HOOK_ENABLED(PropertyComponent.LOGGER_CONTEXT, "shutdownHookEnabled"),
   STACKTRACE_ON_START(PropertyComponent.LOGGER_CONTEXT, "stacktraceOnStart"),
   STATUS_DEFAULT_LEVEL(PropertyComponent.STATUS_LOGGER, "defaultStatusLevel"),
   THREAD_CONTEXT_DATA_CLASS_NAME(PropertyComponent.THREAD_CONTEXT, "contextData"),
   THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME(PropertyComponent.THREAD_CONTEXT, "contextDataInjector"),
   TRANSPORT_SECURITY_TRUST_STORE_LOCATION(PropertyComponent.TRANSPORT_SECURITY, "trustStoreLocation"),
   TRANSPORT_SECURITY_TRUST_STORE_PASSWORD(PropertyComponent.TRANSPORT_SECURITY, "trustStorePassword"),
   TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_FILE(PropertyComponent.TRANSPORT_SECURITY, "trustStorePasswordFile"),
   TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_ENV_VAR(PropertyComponent.TRANSPORT_SECURITY, "trustStorePasswordEnvironmentVariable"),
   TRANSPORT_SECURITY_TRUST_STORE_TYPE(PropertyComponent.TRANSPORT_SECURITY, "trustStoreType"),
   TRANSPORT_SECURITY_TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM(PropertyComponent.TRANSPORT_SECURITY, "trustStoreKeyManagerFactoryAlgorithm"),
   TRANSPORT_SECURITY_KEY_STORE_LOCATION(PropertyComponent.TRANSPORT_SECURITY, "keyStoreLocation"),
   TRANSPORT_SECURITY_KEY_STORE_PASSWORD(PropertyComponent.TRANSPORT_SECURITY, "keystorePassword"),
   TRANSPORT_SECURITY_KEY_STORE_PASSWORD_FILE(PropertyComponent.TRANSPORT_SECURITY, "keyStorePasswordFile"),
   TRANSPORT_SECURITY_KEY_STORE_PASSWORD_ENV_VAR(PropertyComponent.TRANSPORT_SECURITY, "keyStorePasswordEnvironmentVariable"),
   TRANSPORT_SECURITY_KEY_STORE_TYPE(PropertyComponent.TRANSPORT_SECURITY, "keyStoreType"),
   TRANSPORT_SECURITY_KEY_STORE_KEY_MANAGER_FACTORY_ALGORITHM(PropertyComponent.TRANSPORT_SECURITY, "keyStoreKeyManagerFactoryAlgorithm"),
   TRANSPORT_SECURITY_VERIFY_HOST_NAME(PropertyComponent.TRANSPORT_SECURITY, "sslVerifyHostName"),
   USE_PRECISE_CLOCK(PropertyComponent.CONFIGURATION, "usePreciseClock"),
   UUID_SEQUENCE(PropertyComponent.UUID, "sequence");

   private final PropertyComponent component;
   private final String name;
   private final String key;
   private final String systemKey;

   Log4jPropertyKey(final PropertyComponent component, final String name) {
      this.component = component;
      this.name = name;
      this.key = component.getName() + "." + name;
      this.systemKey = "log4j2.*." + this.key;
   }

   public static PropertyKey findKey(final String component, final String name) {
      for (PropertyKey key : values()) {
         if (key.getComponent().equals(component) && key.getName().equals(name)) {
            return key;
         }
      }

      return null;
   }

   public String getComponent() {
      return this.component.getName();
   }

   public String getName() {
      return this.name;
   }

   public String getKey() {
      return this.key;
   }

   public String getSystemKey() {
      return this.systemKey;
   }

   @Override
   public String toString() {
      return this.getKey();
   }

   public static class Constant {
      private static final String DELIM = ".";
      static final String EXCEPTION_HANDLER = "exceptionHandler";
      public static final String ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME = "log4j2.*.AsyncLoggerConfig.exceptionHandler";
      static final String RETRIES = "retries";
      public static final String ASYNC_CONFIG_RETRIES = "log4j2.*.AsyncLoggerConfig.retries";
      static final String RING_BUFFER_SIZE = "ringBufferSize";
      public static final String ASYNC_CONFIG_RING_BUFFER_SIZE = "log4j2.*.AsyncLoggerConfig.ringBufferSize";
      static final String SLEEP_TIME_NS = "sleepTimeNS";
      public static final String ASYNC_CONFIG_SLEEP_TIME_NS = "log4j2.*.AsyncLoggerConfig.sleepTimeNS";
      static final String TIMEOUT = "timeout";
      public static final String ASYNC_CONFIG_TIMEOUT = "log4j2.*.AsyncLoggerConfig.timeout";
      static final String WAIT_STRATEGY = "waitStrategy";
      public static final String ASYNC_CONFIG_WAIT_STRATEGY = "log4j2.*.AsyncLoggerConfig.waitStrategy";
      static final String SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = "synchronizeEnqueueWhenQueueFull";
      public static final String ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = "log4j2.*.AsyncLoggerConfig.synchronizeEnqueueWhenQueueFull";
      static final String DISCARD_THRESHOLD = "discardThreshold";
      public static final String ASYNC_LOGGER_DISCARD_THRESHOLD = "log4j2.*.AsyncLogger.discardThreshold";
      public static final String ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME = "log4j2.*.AsyncLogger.exceptionHandler";
      static final String FORMAT_MSG = "formatMsg";
      public static final String ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND = "log4j2.*.AsyncLogger.formatMsg";
      static final String QUEUE_FULL_POLICY = "queueFullPolicy";
      public static final String ASYNC_LOGGER_QUEUE_FULL_POLICY = "log4j2.*.AsyncLogger.queueFullPolicy";
      public static final String ASYNC_LOGGER_RETRIES = "log4j2.*.AsyncLogger.retries";
      public static final String ASYNC_LOGGER_RING_BUFFER_SIZE = "log4j2.*.AsyncLogger.ringBufferSize";
      public static final String ASYNC_LOGGER_SLEEP_TIME_NS = "log4j2.*.AsyncLogger.sleepTimeNS";
      public static final String ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = "log4j2.*.AsyncLogger.synchronizeEnqueueWhenQueueFull";
      static final String THREAD_NAME_STRATEGY = "threadNameStrategy";
      public static final String ASYNC_LOGGER_THREAD_NAME_STRATEGY = "log4j2.*.AsyncLogger.threadNameStrategy";
      public static final String ASYNC_LOGGER_TIMEOUT = "log4j2.*.AsyncLogger.timeout";
      public static final String ASYNC_LOGGER_WAIT_STRATEGY = "log4j2.*.AsyncLogger.waitStrategy";
      static final String AUTH_PROVIDER = "authorizationProvider";
      public static final String CONFIG_AUTH_PROVIDER = "log4j2.*." + PropertyComponent.CONFIGURATION + ".authorizationProvider";
      static final String CLOCK = "clock";
      public static final String CONFIG_CLOCK = "log4j2.*.Configuration.clock";
      static final String CONFIGURATION_FACTORY = "configurationFactory";
      public static final String CONFIG_CONFIGURATION_FACTORY_CLASS_NAME = "log4j2.*.Configuration.configurationFactory";
      static final String LEVEL = "level";
      public static final String CONFIG_DEFAULT_LEVEL = "log4j2.*.Configuration.level";
      static final String FILE = "file";
      public static final String CONFIG_LOCATION = "log4j2.*.Configuration.file";
      static final String JANSI_ENABLED = "jansiEnabled";
      public static final String CONFIG_JANSI_ENABLED = "log4j2.*.Configuration.jansiEnabled";
      static final String MERGE_STRATEGY = "mergeStrategy";
      public static final String CONFIG_MERGE_STRATEGY = "log4j2.*.Configuration.mergeStrategy";
      static final String RELIABILITY_STRATEGY = "reliabilityStrategy";
      public static final String CONFIG_RELIABILITY_STRATEGY = "log4j2.*.Configuration.reliabilityStrategy";
      static final String WAIT_MILLIS_BEFORE_STOP_OLD_CONFIG = "waitMillisBeforeStopOldConfig";
      public static final String CONFIG_RELIABILITY_STRATEGY_AWAIT_UNCONDITIONALLY_MILLIS = "log4j2.*.Configuration.waitMillisBeforeStopOldConfig";
      static final String COMPATIBILITY = "compatibility";
      public static final String CONFIG_V1_COMPATIBILITY_ENABLED = "log4j2.*.log4j1.compatibility";
      static final String CONFIGURATION = "configuration";
      public static final String CONFIG_V1_FILE_NAME = "log4j2.*.log4j1.configuration";
      static final String SELECTOR = "selector";
      public static final String CONTEXT_SELECTOR_CLASS_NAME = "log4j2.*.LoggerContext.selector";
      static final String ENABLE_DIRECT_ENCODERS = "enableDirectEncoders";
      public static final String GC_ENABLE_DIRECT_ENCODERS = "log4j2.*.GC.enableDirectEncoders";
      static final String ENCODER_BYTE_BUFFER_SIZE = "encoderByteBufferSize";
      public static final String GC_ENCODER_BYTE_BUFFER_SIZE = "log4j2.*.GC.encoderByteBufferSize";
      static final String ENCODER_CHAR_BUFFER_SIZE = "encoderCharBufferSize";
      public static final String GC_ENCODER_CHAR_BUFFER_SIZE = "log4j2.*.GC.encoderCharBufferSize";
      static final String INITIAL_REUSABLE_MSG_SIZE = "initialReusableMsgSize";
      public static final String GC_INITIAL_REUSABLE_MESSAGE_SIZE = "log4j2.*.GC.initialReusableMsgSize";
      static final String LAYOUT_STRINGBUILDER_MAX_SIZE = "layoutStringBuilderMaxSize";
      public static final String GC_LAYOUT_STRING_BUILDER_MAX_SIZE = "log4j2.*.GC.layoutStringBuilderMaxSize";
      static final String MAX_REUSABLE_MSG_SIZE = "maxReusableMsgSize";
      public static final String GC_REUSABLE_MESSAGE_MAX_SIZE = "log4j2.*.GC.maxReusableMsgSize";
      static final String ENABLED = "enabled";
      public static final String JMX_ENABLED = "log4j2.*.JMX.enabled";
      static final String NOTIFY_ASYNC = "notifyAsync";
      public static final String JMX_NOTIFY_ASYNC = "log4j2.*.JMX.notifyAsync";
      static final String CONTEXT_SELECTOR = "contextSelector";
      public static final String JNDI_CONTEXT_SELECTOR = "log4j2.*.JNDI.contextSelector";
      static final String ENABLE_JDBC = "enableJDBC";
      public static final String JNDI_ENABLE_JDBC = "log4j2.*.JNDI.enableJDBC";
      static final String ENABLE_JMS = "enableJMS";
      public static final String JNDI_ENABLE_JMS = "log4j2.*.JNDI.enableJMS";
      static final String ENABLE_LOOKUP = "enableLookup";
      public static final String JNDI_ENABLE_LOOKUP = "log4j2.*.JNDI.enableLookup";
      static final String LOG_EVENT_FACTORY = "logEventFactory";
      public static final String LOG_EVENT_FACTORY_CLASS_NAME = "log4j2.*.LoggerContext.logEventFactory";
      static final String SHUT_DOWN_CALLBACK_REGISTRY = "shutdownCallbackRegistry";
      public static final String SHUTDOWN_CALLBACK_REGISTRY = "log4j2.*.LoggerContext.shutdownCallbackRegistry";
      static final String SHUT_DOWN_HOOK_ENABLED = "shutdownHookEnabled";
      public static final String SHUTDOWN_HOOK_ENABLED = "log4j2.*.LoggerContext.shutdownHookEnabled";
      static final String STACK_TRACE_ON_START = "stacktraceOnStart";
      public static final String STACKTRACE_ON_START = "log4j2.*.LoggerContext.stacktraceOnStart";
      static final String DEFAULT_STATUS_LEVEL = "defaultStatusLevel";
      public static final String STATUS_DEFAULT_LEVEL = "log4j2.*.StatusLogger.defaultStatusLevel";
      static final String CONTEXT_DATA = "contextData";
      public static final String THREAD_CONTEXT_DATA_CLASS_NAME = "log4j2.*.ThreadContext.contextData";
      static final String CONTEXT_DATA_INJECTOR = "contextDataInjector";
      public static final String THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME = "log4j2.*.ThreadContext.contextDataInjector";
      static final String TRUST_STORE_LOCATION = "trustStoreLocation";
      public static final String TRANSPORT_SECURITY_TRUST_STORE_LOCATION = "log4j2.*.TransportSecurity.trustStoreLocation";
      static final String TRUST_STORE_PASSWORD = "trustStorePassword";
      public static final String TRANSPORT_SECURITY_TRUST_STORE_PASSWORD = "log4j2.*.TransportSecurity.trustStorePassword";
      static final String TRUST_STORE_PASSWORD_FILE = "trustStorePasswordFile";
      public static final String TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_FILE = "log4j2.*.TransportSecurity.trustStorePasswordFile";
      static final String TRUST_STORE_PASSWORD_ENVIRONMENT_VARIABLE = "trustStorePasswordEnvironmentVariable";
      public static final String TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_ENV_VAR = "log4j2.*.TransportSecurity.trustStorePasswordEnvironmentVariable";
      static final String TRUST_STORE_TYPE = "trustStoreType";
      public static final String TRANSPORT_SECURITY_TRUST_STORE_TYPE = "log4j2.*.TransportSecurity.trustStoreType";
      static final String TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM = "trustStoreKeyManagerFactoryAlgorithm";
      public static final String TRANSPORT_SECURITY_TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM = "log4j2.*.TransportSecurity.trustStoreKeyManagerFactoryAlgorithm";
      static final String KEYSTORE_LOCATION = "keyStoreLocation";
      public static final String TRANSPORT_SECURITY_KEY_STORE_LOCATION = "log4j2.*.TransportSecurity.keyStoreLocation";
      static final String KEYSTORE_PASSWORD = "keystorePassword";
      public static final String TRANSPORT_SECURITY_KEY_STORE_PASSWORD = "log4j2.*.TransportSecurity.keystorePassword";
      static final String KEYSTORE_PASSWORD_FILE = "keyStorePasswordFile";
      public static final String TRANSPORT_SECURITY_KEY_STORE_PASSWORD_FILE = "log4j2.*.TransportSecurity.keyStorePasswordFile";
      static final String KEYSTORE_PASSWORD_ENVIRONMENT_VARIABLE = "keyStorePasswordEnvironmentVariable";
      public static final String TRANSPORT_SECURITY_KEY_STORE_PASSWORD_ENV_VAR = "log4j2.*.TransportSecurity.keyStorePasswordEnvironmentVariable";
      static final String KEYSTORE_TYPE = "keyStoreType";
      public static final String TRANSPORT_SECURITY_KEY_STORE_TYPE = "log4j2.*.TransportSecurity.keyStoreType";
      static final String KEYSTORE_KEY_MANAGER_FACTORY_ALGORITHM = "keyStoreKeyManagerFactoryAlgorithm";
      public static final String TRANSPORT_SECURITY_KEY_STORE_KEY_MANAGER_FACTORY_ALGORITHM = "log4j2.*.TransportSecurity.keyStoreKeyManagerFactoryAlgorithm";
      static final String SSL_VERIFY_HOST_NAME = "sslVerifyHostName";
      public static final String TRANSPORT_SECURITY_VERIFY_HOST_NAME = "log4j2.*.TransportSecurity.sslVerifyHostName";
      static final String USE_PRECISE_cLOCK = "usePreciseClock";
      public static final String USE_PRECISE_CLOCK = "log4j2.*.Configuration.usePreciseClock";
      static final String SEQUENCE = "sequence";
      public static final String UUID_SEQUENCE = "log4j2.*.UUID.sequence";
   }
}
