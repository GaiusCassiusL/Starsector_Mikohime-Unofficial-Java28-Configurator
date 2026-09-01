package org.apache.logging.log4j.core.selector;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.spi.LoggerContextShutdownAware;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.StackLocatorUtil;

@Singleton
public class ClassLoaderContextSelector implements ContextSelector, LoggerContextShutdownAware {
   protected static final StatusLogger LOGGER = StatusLogger.getLogger();
   protected final Lazy<LoggerContext> defaultContext = Lazy.lazy(() -> this.createContext(this.defaultContextName(), null));
   protected final Map<String, AtomicReference<WeakReference<LoggerContext>>> contextMap = new ConcurrentHashMap<>();
   protected final Injector injector;

   @Inject
   public ClassLoaderContextSelector(final Injector injector) {
      this.injector = injector;
   }

   @Override
   public void shutdown(final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
      LoggerContext ctx = null;
      if (currentContext) {
         ctx = ContextAnchor.THREAD_CONTEXT.get();
      } else if (loader != null) {
         ctx = this.findContext(loader);
      } else {
         Class<?> clazz = StackLocatorUtil.getCallerClass(fqcn);
         if (clazz != null) {
            ctx = this.findContext(clazz.getClassLoader());
         }

         if (ctx == null) {
            ctx = ContextAnchor.THREAD_CONTEXT.get();
         }
      }

      if (ctx != null) {
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
      LoggerContext ctx;
      if (currentContext) {
         ctx = ContextAnchor.THREAD_CONTEXT.get();
      } else if (loader != null) {
         ctx = this.findContext(loader);
      } else {
         Class<?> clazz = StackLocatorUtil.getCallerClass(fqcn);
         if (clazz != null) {
            ctx = this.findContext(clazz.getClassLoader());
         } else {
            ctx = ContextAnchor.THREAD_CONTEXT.get();
         }
      }

      return ctx != null && ctx.isStarted();
   }

   private LoggerContext findContext(final ClassLoader loaderOrNull) {
      ClassLoader loader = loaderOrNull != null ? loaderOrNull : ClassLoader.getSystemClassLoader();
      String name = this.toContextMapKey(loader);
      AtomicReference<WeakReference<LoggerContext>> ref = this.contextMap.get(name);
      if (ref != null) {
         WeakReference<LoggerContext> weakRef = ref.get();
         return weakRef.get();
      } else {
         return null;
      }
   }

   @Override
   public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
      return this.getContext(fqcn, loader, currentContext, null);
   }

   @Override
   public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext, final URI configLocation) {
      return this.getContext(fqcn, loader, null, currentContext, configLocation);
   }

   @Override
   public LoggerContext getContext(
      final String fqcn, final ClassLoader loader, final Entry<String, Object> entry, final boolean currentContext, final URI configLocation
   ) {
      if (currentContext) {
         LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
         return ctx != null ? ctx : this.getDefault();
      }

      if (loader != null) {
         return this.locateContext(loader, entry, configLocation);
      }

      Class<?> clazz = StackLocatorUtil.getCallerClass(fqcn);
      if (clazz != null) {
         return this.locateContext(clazz.getClassLoader(), entry, configLocation);
      }

      LoggerContext lc = ContextAnchor.THREAD_CONTEXT.get();
      return lc != null ? lc : this.getDefault();
   }

   @Override
   public void removeContext(final LoggerContext context) {
      for (Entry<String, AtomicReference<WeakReference<LoggerContext>>> entry : this.contextMap.entrySet()) {
         LoggerContext ctx = entry.getValue().get().get();
         if (ctx == context) {
            this.contextMap.remove(entry.getKey());
         }
      }
   }

   @Override
   public boolean isClassLoaderDependent() {
      return true;
   }

   @Override
   public List<LoggerContext> getLoggerContexts() {
      List<LoggerContext> list = new ArrayList<>();

      for (AtomicReference<WeakReference<LoggerContext>> ref : this.contextMap.values()) {
         LoggerContext ctx = ref.get().get();
         if (ctx != null) {
            list.add(ctx);
         }
      }

      return Collections.unmodifiableList(list);
   }

   private Injector getOrCopyInjector(final Entry<String, Object> entry) {
      if (entry != null) {
         Object value = entry.getValue();
         if (value instanceof Injector) {
            return (Injector)value;
         }
      }

      Injector injector = this.injector;
      return injector != null ? injector.copy() : null;
   }

   private LoggerContext locateContext(final ClassLoader loaderOrNull, final Entry<String, Object> entry, final URI configLocation) {
      ClassLoader loader = loaderOrNull != null ? loaderOrNull : ClassLoader.getSystemClassLoader();
      String name = this.toContextMapKey(loader);
      AtomicReference<WeakReference<LoggerContext>> ref = this.contextMap.get(name);
      if (ref == null) {
         if (configLocation == null) {
            for (ClassLoader parent = loader.getParent(); parent != null; parent = parent.getParent()) {
               ref = this.contextMap.get(this.toContextMapKey(parent));
               if (ref != null) {
                  WeakReference<LoggerContext> r = ref.get();
                  LoggerContext ctx = r.get();
                  if (ctx != null) {
                     return ctx;
                  }
               }
            }
         }

         LoggerContext ctx = this.createContext(name, configLocation, this.getOrCopyInjector(entry));
         if (entry != null) {
            ctx.putObject(entry.getKey(), entry.getValue());
         }

         LoggerContext newContext = this.contextMap.computeIfAbsent(name, k -> new AtomicReference<>(new WeakReference<>(ctx))).get().get();
         if (newContext != null && newContext == ctx) {
            newContext.addShutdownListener(this);
         }

         return newContext;
      } else {
         WeakReference<LoggerContext> weakRef = ref.get();
         LoggerContext ctx = weakRef.get();
         if (ctx != null) {
            if (entry != null) {
               ctx.putObject(entry.getKey(), entry.getValue());
            }

            if (ctx.getConfigLocation() == null && configLocation != null) {
               LOGGER.debug("Setting configuration to {}", configLocation);
               ctx.setConfigLocation(configLocation);
            } else if (ctx.getConfigLocation() != null && configLocation != null && !ctx.getConfigLocation().equals(configLocation)) {
               LOGGER.warn("locateContext called with URI {}. Existing LoggerContext has URI {}", configLocation, ctx.getConfigLocation());
            }

            return ctx;
         } else {
            ctx = this.createContext(name, configLocation, this.getOrCopyInjector(entry));
            if (entry != null) {
               ctx.putObject(entry.getKey(), entry.getValue());
            }

            if (ref.compareAndSet(weakRef, new WeakReference<>(ctx))) {
               ctx.addShutdownListener(this);
            }

            return ctx;
         }
      }
   }

   protected LoggerContext createContext(final String name, final URI configLocation) {
      Injector injector = this.injector;
      return this.createContext(name, configLocation, injector != null ? injector.copy() : null);
   }

   protected LoggerContext createContext(final String name, final URI configLocation, final Injector injector) {
      return new LoggerContext(name, null, configLocation, injector);
   }

   protected String toContextMapKey(final ClassLoader loader) {
      return Integer.toHexString(System.identityHashCode(loader));
   }

   protected LoggerContext getDefault() {
      return (LoggerContext)this.defaultContext.value();
   }

   protected String defaultContextName() {
      return "Default";
   }
}
