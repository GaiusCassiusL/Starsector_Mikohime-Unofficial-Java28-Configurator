package org.apache.logging.log4j.core.appender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;

public abstract class AbstractManager implements AutoCloseable {
   protected static final Logger LOGGER = StatusLogger.getLogger();
   private static final Map<String, AbstractManager> MAP = new HashMap<>();
   private static final Lock LOCK = new ReentrantLock();
   protected int count;
   private final String name;
   private final LoggerContext loggerContext;

   protected AbstractManager(final LoggerContext loggerContext, final String name) {
      this.loggerContext = loggerContext;
      this.name = name;
      LOGGER.debug("Starting {} {}", this.getClass().getSimpleName(), name);
   }

   @Override
   public void close() {
      this.stop(0L, AbstractLifeCycle.DEFAULT_STOP_TIMEUNIT);
   }

   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      boolean stopped = true;
      LOCK.lock();

      try {
         this.count--;
         if (this.count <= 0) {
            MAP.remove(this.name);
            LOGGER.debug("Shutting down {} {}", this.getClass().getSimpleName(), this.getName());
            stopped = this.releaseSub(timeout, timeUnit);
            LOGGER.debug("Shut down {} {}, all resources released: {}", this.getClass().getSimpleName(), this.getName(), stopped);
         }
      } finally {
         LOCK.unlock();
      }

      return stopped;
   }

   public static <M extends AbstractManager, T> M getManager(final String name, final ManagerFactory<M, T> factory, final T data) {
      LOCK.lock();

      try {
         M manager = (M)MAP.get(name);
         if (manager == null) {
            manager = (M)factory.createManager(name, data);
            if (manager == null) {
               throw new IllegalStateException("ManagerFactory [" + factory + "] unable to create manager for [" + name + "] with data [" + data + "]");
            }

            MAP.put(name, manager);
         } else {
            manager.updateData(data);
         }

         manager.count++;
         return manager;
      } finally {
         LOCK.unlock();
      }
   }

   public void updateData(final Object data) {
   }

   public static boolean hasManager(final String name) {
      LOCK.lock();

      try {
         return MAP.containsKey(name);
      } finally {
         LOCK.unlock();
      }
   }

   protected static <M extends AbstractManager> M narrow(final Class<M> narrowClass, final AbstractManager manager) {
      if (narrowClass.isAssignableFrom(manager.getClass())) {
         return (M)manager;
      } else {
         throw new ConfigurationException("Configuration has multiple incompatible Appenders pointing to the same resource '" + manager.getName() + "'");
      }
   }

   protected static StatusLogger logger() {
      return StatusLogger.getLogger();
   }

   protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
      return true;
   }

   protected int getCount() {
      return this.count;
   }

   public LoggerContext getLoggerContext() {
      return this.loggerContext;
   }

   public String getName() {
      return this.name;
   }

   public Map<String, String> getContentFormat() {
      return new HashMap<>();
   }

   protected void log(final Level level, final String message, final Throwable throwable) {
      Message m = LOGGER.getMessageFactory().newMessage("{} {} {}: {}", this.getClass().getSimpleName(), this.getName(), message, throwable);
      LOGGER.log(level, m, throwable);
   }

   protected void logDebug(final String message, final Throwable throwable) {
      this.log(Level.DEBUG, message, throwable);
   }

   protected void logError(final String message, final Throwable throwable) {
      this.log(Level.ERROR, message, throwable);
   }

   protected void logWarn(final String message, final Throwable throwable) {
      this.log(Level.WARN, message, throwable);
   }
}
