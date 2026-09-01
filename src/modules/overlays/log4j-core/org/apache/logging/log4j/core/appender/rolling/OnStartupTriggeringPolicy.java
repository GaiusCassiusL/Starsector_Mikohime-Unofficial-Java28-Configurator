package org.apache.logging.log4j.core.appender.rolling;

import java.lang.reflect.Method;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(printObject = true)
@Plugin
public final class OnStartupTriggeringPolicy extends AbstractTriggeringPolicy {
   private static final long JVM_START_TIME = initStartTime();
   private final long minSize;

   private OnStartupTriggeringPolicy(final long minSize) {
      this.minSize = minSize;
   }

   private static long initStartTime() {
      try {
         Class<?> factoryClass = Loader.loadSystemClass("java.lang.management.ManagementFactory");
         Method getRuntimeMXBean = factoryClass.getMethod("getRuntimeMXBean");
         Object runtimeMXBean = getRuntimeMXBean.invoke(null);
         Class<?> runtimeMXBeanClass = Loader.loadSystemClass("java.lang.management.RuntimeMXBean");
         Method getStartTime = runtimeMXBeanClass.getMethod("getStartTime");
         return (Long)getStartTime.invoke(runtimeMXBean);
      } catch (Throwable t) {
         StatusLogger.getLogger()
            .error("Unable to call ManagementFactory.getRuntimeMXBean().getStartTime(), using system time for OnStartupTriggeringPolicy", t);
         return System.currentTimeMillis();
      }
   }

   @Override
   public void initialize(final RollingFileManager manager) {
      if (manager.getFileTime() < JVM_START_TIME && manager.getFileSize() >= this.minSize) {
         StatusLogger.getLogger().debug("Initiating rollover at startup");
         if (this.minSize == 0L) {
            manager.setRenameEmptyFiles(true);
         }

         manager.skipFooter(true);
         manager.rollover();
         manager.skipFooter(false);
      }
   }

   @Override
   public boolean isTriggeringEvent(final LogEvent event) {
      return false;
   }

   @Override
   public String toString() {
      return "OnStartupTriggeringPolicy";
   }

   @PluginFactory
   public static OnStartupTriggeringPolicy createPolicy(@PluginAttribute(defaultLong = 1L) final long minSize) {
      return new OnStartupTriggeringPolicy(minSize);
   }
}
