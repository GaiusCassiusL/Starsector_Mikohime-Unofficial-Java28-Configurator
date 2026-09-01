package org.apache.log4j;

import org.apache.log4j.spi.LoggerFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.StackLocatorUtil;

public class Logger extends Category {
   private static final String FQCN = Logger.class.getName();

   public static Logger getLogger(final Class<?> clazz) {
      return LogManager.getLogger(clazz.getName(), StackLocatorUtil.getCallerClassLoader(2));
   }

   public static Logger getLogger(final String name) {
      return LogManager.getLogger(name, StackLocatorUtil.getCallerClassLoader(2));
   }

   public static Logger getLogger(final String name, final LoggerFactory factory) {
      return LogManager.getLogger(name, factory, StackLocatorUtil.getCallerClassLoader(2));
   }

   public static Logger getRootLogger() {
      return LogManager.getRootLogger();
   }

   Logger(final LoggerContext context, final String name) {
      super(context, name);
   }

   protected Logger(final String name) {
      super(name);
   }

   public boolean isTraceEnabled() {
      return this.getLogger().isTraceEnabled();
   }

   public void trace(final Object message) {
      this.maybeLog(FQCN, org.apache.logging.log4j.Level.TRACE, message, null);
   }

   public void trace(final Object message, final Throwable t) {
      this.maybeLog(FQCN, org.apache.logging.log4j.Level.TRACE, message, t);
   }
}
