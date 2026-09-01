package org.apache.logging.log4j.core.selector;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.spi.LoggerContextShutdownAware;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;

@Singleton
public class BasicContextSelector implements ContextSelector, LoggerContextShutdownAware {
   private static final Logger LOGGER = StatusLogger.getLogger();
   protected final Lazy<LoggerContext> context = Lazy.lazy(this::createContext);
   protected final Injector injector;

   @Inject
   public BasicContextSelector(final Injector injector) {
      this.injector = injector;
   }

   @Override
   public void shutdown(final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
      LoggerContext ctx = this.getContext(fqcn, loader, currentContext);
      if (ctx != null && ctx.isStarted()) {
         ctx.stop(50L, TimeUnit.MILLISECONDS);
      }
   }

   public void contextShutdown(final org.apache.logging.log4j.spi.LoggerContext loggerContext) {
      if (loggerContext instanceof LoggerContext) {
         this.removeContext((LoggerContext)loggerContext);
      }
   }

   @Override
   public boolean hasContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
      LoggerContext ctx = this.getContext(fqcn, loader, currentContext);
      return ctx != null && ctx.isStarted();
   }

   @Override
   public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
      return this.getContext(fqcn, loader, currentContext, null);
   }

   @Override
   public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext, final URI configLocation) {
      if (currentContext) {
         LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
         if (ctx != null) {
            return ctx;
         }
      }

      LoggerContext ctx = (LoggerContext)this.context.get();
      if (configLocation != null) {
         if (ctx.getConfigLocation() == null) {
            LOGGER.debug("Setting configuration to {}", configLocation);
            ctx.setConfigLocation(configLocation);
         } else if (!ctx.getConfigLocation().equals(configLocation)) {
            LOGGER.warn("getContext called with URI {}. Existing LoggerContext has URI {}", configLocation, ctx.getConfigLocation());
         }
      }

      return ctx;
   }

   @Override
   public void removeContext(final LoggerContext context) {
      if (context == this.context.get()) {
         this.context.set(null);
      }
   }

   @Override
   public boolean isClassLoaderDependent() {
      return false;
   }

   @Override
   public List<LoggerContext> getLoggerContexts() {
      return List.of((LoggerContext)this.context.value());
   }

   protected LoggerContext createContext() {
      return new LoggerContext("Default", null, (URI)null, this.injector);
   }
}
