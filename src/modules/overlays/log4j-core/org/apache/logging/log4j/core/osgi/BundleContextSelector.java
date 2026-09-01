package org.apache.logging.log4j.core.osgi;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;

@Singleton
public class BundleContextSelector extends ClassLoaderContextSelector {
   @Inject
   public BundleContextSelector(final Injector injector) {
      super(injector);
   }

   @Override
   public void shutdown(final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
      LoggerContext ctx = null;
      Bundle bundle = null;
      if (currentContext) {
         ctx = ContextAnchor.THREAD_CONTEXT.get();
         ContextAnchor.THREAD_CONTEXT.remove();
      }

      if (ctx == null && loader instanceof BundleReference) {
         bundle = ((BundleReference)loader).getBundle();
         ctx = this.getLoggerContext(bundle);
         this.removeLoggerContext(ctx);
      }

      if (ctx == null) {
         Class<?> callerClass = StackLocatorUtil.getCallerClass(fqcn);
         if (callerClass != null) {
            bundle = FrameworkUtil.getBundle(callerClass);
            ctx = this.getLoggerContext(FrameworkUtil.getBundle(callerClass));
            this.removeLoggerContext(ctx);
         }
      }

      if (ctx == null) {
         ctx = ContextAnchor.THREAD_CONTEXT.get();
         ContextAnchor.THREAD_CONTEXT.remove();
      }

      if (ctx != null) {
         ctx.stop(50L, TimeUnit.MILLISECONDS);
      }

      if (bundle != null && allContexts) {
         Bundle[] bundles = bundle.getBundleContext().getBundles();

         for (Bundle bdl : bundles) {
            ctx = this.getLoggerContext(bdl);
            if (ctx != null) {
               ctx.stop(50L, TimeUnit.MILLISECONDS);
            }
         }
      }
   }

   private LoggerContext getLoggerContext(final Bundle bundle) {
      String name = Objects.requireNonNull(bundle, "No Bundle provided").getSymbolicName();
      AtomicReference<WeakReference<LoggerContext>> ref = this.contextMap.get(name);
      return ref != null && ref.get() != null ? ref.get().get() : null;
   }

   private void removeLoggerContext(final LoggerContext context) {
      this.contextMap.remove(context.getName());
   }

   @Override
   public boolean hasContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
      if (currentContext && ContextAnchor.THREAD_CONTEXT.get() != null) {
         return ContextAnchor.THREAD_CONTEXT.get().isStarted();
      }

      if (loader instanceof BundleReference) {
         return this.hasContext(((BundleReference)loader).getBundle());
      }

      Class<?> callerClass = StackLocatorUtil.getCallerClass(fqcn);
      return callerClass != null
         ? this.hasContext(FrameworkUtil.getBundle(callerClass))
         : ContextAnchor.THREAD_CONTEXT.get() != null && ContextAnchor.THREAD_CONTEXT.get().isStarted();
   }

   @Override
   public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext, final URI configLocation) {
      if (currentContext) {
         LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
         return ctx != null ? ctx : this.getDefault();
      }

      if (loader instanceof BundleReference) {
         return this.locateContext(((BundleReference)loader).getBundle(), configLocation);
      }

      Class<?> callerClass = StackLocatorUtil.getCallerClass(fqcn);
      if (callerClass != null) {
         return this.locateContext(FrameworkUtil.getBundle(callerClass), configLocation);
      }

      LoggerContext lc = ContextAnchor.THREAD_CONTEXT.get();
      return lc == null ? this.getDefault() : lc;
   }

   private boolean hasContext(final Bundle bundle) {
      String name = Objects.requireNonNull(bundle, "No Bundle provided").getSymbolicName();
      AtomicReference<WeakReference<LoggerContext>> ref = this.contextMap.get(name);
      return ref != null && ref.get() != null && ref.get().get() != null && ref.get().get().isStarted();
   }

   private LoggerContext locateContext(final Bundle bundle, final URI configLocation) {
      String name = Objects.requireNonNull(bundle, "No Bundle provided").getSymbolicName();
      AtomicReference<WeakReference<LoggerContext>> ref = this.contextMap.get(name);
      if (ref == null) {
         LoggerContext context = new LoggerContext(name, bundle, configLocation);
         this.contextMap.putIfAbsent(name, new AtomicReference<>(new WeakReference<>(context)));
         return this.contextMap.get(name).get().get();
      }

      WeakReference<LoggerContext> r = ref.get();
      LoggerContext ctx = r.get();
      if (ctx == null) {
         LoggerContext context = new LoggerContext(name, bundle, configLocation);
         ref.compareAndSet(r, new WeakReference<>(context));
         return ref.get().get();
      }

      URI oldConfigLocation = ctx.getConfigLocation();
      if (oldConfigLocation == null && configLocation != null) {
         LOGGER.debug("Setting bundle ({}) configuration to {}", name, configLocation);
         ctx.setConfigLocation(configLocation);
      } else if (oldConfigLocation != null && configLocation != null && !configLocation.equals(oldConfigLocation)) {
         LOGGER.warn("locateContext called with URI [{}], but existing LoggerContext has URI [{}]", configLocation, oldConfigLocation);
      }

      return ctx;
   }
}
