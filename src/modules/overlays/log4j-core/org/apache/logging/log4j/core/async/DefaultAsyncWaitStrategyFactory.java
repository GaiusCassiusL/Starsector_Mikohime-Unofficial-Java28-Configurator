package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyKey;
import org.apache.logging.log4j.util.Strings;

class DefaultAsyncWaitStrategyFactory implements AsyncWaitStrategyFactory {
   static final String DEFAULT_WAIT_STRATEGY_CLASSNAME = TimeoutBlockingWaitStrategy.class.getName();
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final PropertyKey propertyKey;

   public DefaultAsyncWaitStrategyFactory(PropertyKey key) {
      this.propertyKey = key;
   }

   @Override
   public WaitStrategy createWaitStrategy() {
      String strategy = PropertiesUtil.getProperties().getStringProperty(this.propertyKey, "TIMEOUT");
      LOGGER.trace("DefaultAsyncWaitStrategyFactory property {}={}", this.propertyKey, strategy);
      String strategyUp = Strings.toRootUpperCase(strategy);
      switch (strategyUp) {
         case "SLEEP":
            String component = this.propertyKey.getComponent();
            PropertyKey key = Log4jPropertyKey.findKey(component, "sleepTimeNs");
            long sleepTimeNs = PropertiesUtil.getProperties().getLongProperty(key, 100L);
            key = Log4jPropertyKey.findKey(component, "retries");
            int retries = PropertiesUtil.getProperties().getIntegerProperty(key, 200);
            LOGGER.trace("DefaultAsyncWaitStrategyFactory creating SleepingWaitStrategy(retries={}, sleepTimeNs={})", retries, sleepTimeNs);
            return new SleepingWaitStrategy(retries, sleepTimeNs);
         case "YIELD":
            LOGGER.trace("DefaultAsyncWaitStrategyFactory creating YieldingWaitStrategy");
            return new YieldingWaitStrategy();
         case "BLOCK":
            LOGGER.trace("DefaultAsyncWaitStrategyFactory creating BlockingWaitStrategy");
            return new BlockingWaitStrategy();
         case "BUSYSPIN":
            LOGGER.trace("DefaultAsyncWaitStrategyFactory creating BusySpinWaitStrategy");
            return new BusySpinWaitStrategy();
         case "TIMEOUT":
            return createDefaultWaitStrategy(this.propertyKey);
         default:
            return createDefaultWaitStrategy(this.propertyKey);
      }
   }

   static WaitStrategy createDefaultWaitStrategy(final PropertyKey propertyKey) {
      String component = propertyKey.getComponent();
      PropertyKey key = Log4jPropertyKey.findKey(component, "timeout");
      long timeoutMillis = PropertiesUtil.getProperties().getLongProperty(key, 10L);
      LOGGER.trace("DefaultAsyncWaitStrategyFactory creating TimeoutBlockingWaitStrategy(timeout={}, unit=MILLIS)", timeoutMillis);
      return new TimeoutBlockingWaitStrategy(timeoutMillis, TimeUnit.MILLISECONDS);
   }
}
