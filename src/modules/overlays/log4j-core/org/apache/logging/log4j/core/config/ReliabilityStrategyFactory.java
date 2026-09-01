package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

public final class ReliabilityStrategyFactory {
   private ReliabilityStrategyFactory() {
   }

   public static ReliabilityStrategy getReliabilityStrategy(final LoggerConfig loggerConfig) {
      String strategy = PropertiesUtil.getProperties().getStringProperty(Log4jPropertyKey.CONFIG_RELIABILITY_STRATEGY, "AwaitCompletion");
      if ("AwaitCompletion".equals(strategy)) {
         return new AwaitCompletionReliabilityStrategy(loggerConfig);
      }

      if ("AwaitUnconditionally".equals(strategy)) {
         return new AwaitUnconditionallyReliabilityStrategy(loggerConfig);
      }

      if ("Locking".equals(strategy)) {
         return new LockingReliabilityStrategy(loggerConfig);
      }

      try {
         Class<? extends ReliabilityStrategy> cls = Loader.loadClass(strategy).asSubclass(ReliabilityStrategy.class);
         return cls.getConstructor(LoggerConfig.class).newInstance(loggerConfig);
      } catch (Exception dynamicFailed) {
         StatusLogger.getLogger()
            .warn("Could not create ReliabilityStrategy for '{}', using default AwaitCompletionReliabilityStrategy: {}", strategy, dynamicFailed);
         return new AwaitCompletionReliabilityStrategy(loggerConfig);
      }
   }
}
