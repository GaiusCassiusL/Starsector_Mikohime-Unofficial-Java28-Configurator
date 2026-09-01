package org.apache.log4j.bridge;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

public class LogEventAdapter extends LoggingEvent {
   private static final long JVM_START_TIME = initStartTime();
   private final LogEvent event;

   public LogEventAdapter(final LogEvent event) {
      this.event = event;
   }

   public static long getStartTime() {
      return JVM_START_TIME;
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

   public LogEvent getEvent() {
      return this.event;
   }

   @Override
   public LocationInfo getLocationInformation() {
      return new LocationInfo(this.event.getSource());
   }

   @Override
   public Level getLevel() {
      return OptionConverter.convertLevel(this.event.getLevel());
   }

   @Override
   public String getLoggerName() {
      return this.event.getLoggerName();
   }

   @Override
   public long getTimeStamp() {
      return this.event.getTimeMillis();
   }

   @Override
   public Category getLogger() {
      return Category.getInstance(this.event.getLoggerName());
   }

   @Override
   public Object getMessage() {
      return this.event.getMessage();
   }

   @Override
   public String getNDC() {
      return this.event.getContextStack().toString();
   }

   @Override
   public Object getMDC(final String key) {
      return this.event.getContextData() != null ? this.event.getContextData().getValue(key) : null;
   }

   @Override
   public void getMDCCopy() {
   }

   @Override
   public String getRenderedMessage() {
      return this.event.getMessage().getFormattedMessage();
   }

   @Override
   public String getThreadName() {
      return this.event.getThreadName();
   }

   @Override
   public ThrowableInformation getThrowableInformation() {
      return this.event.getThrown() != null ? new ThrowableInformation(this.event.getThrown()) : null;
   }

   @Override
   public String[] getThrowableStrRep() {
      return this.event.getThrown() != null ? Throwables.toStringList(this.event.getThrown()).toArray(Strings.EMPTY_ARRAY) : null;
   }

   @Override
   public String getProperty(final String key) {
      return (String)this.event.getContextData().getValue(key);
   }

   @Override
   public Set getPropertyKeySet() {
      return this.event.getContextData().toMap().keySet();
   }

   @Override
   public Map getProperties() {
      return this.event.getContextData().toMap();
   }
}
