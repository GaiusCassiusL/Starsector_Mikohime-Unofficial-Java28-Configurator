package org.apache.logging.log4j.core.osgi;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.ContextDataProvider;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.InjectorCallback;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginRegistry;
import org.apache.logging.log4j.plugins.model.PluginService;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ServiceRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

public final class Activator implements BundleActivator {
   private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();
   private ServiceRegistration<PluginRegistry> pluginRegistryServiceRegistration;
   private PluginRegistry pluginRegistry;
   private ServiceRegistration<InjectorCallback> injectorCallbackServiceRegistration;
   private InjectorCallback injectorCallback;

   public void start(final BundleContext context) throws Exception {
      this.pluginRegistryServiceRegistration = context.registerService(PluginRegistry.class, new PluginRegistry(), new Hashtable());
      this.pluginRegistry = (PluginRegistry)context.getService(this.pluginRegistryServiceRegistration.getReference());
      Bundle bundle = context.getBundle();
      ClassLoader classLoader = ((BundleWiring)bundle.adapt(BundleWiring.class)).getClassLoader();
      this.injectorCallbackServiceRegistration = context.registerService(
         InjectorCallback.class,
         new InjectorCallback() {
            public void configure(final Injector injector) {
               injector.registerBinding(
                  Key.forClass(PluginRegistry.class), () -> (PluginRegistry)context.getService(Activator.this.pluginRegistryServiceRegistration.getReference())
               );
               if (PropertiesUtil.getProperties().getStringProperty(Log4jPropertyKey.CONTEXT_SELECTOR_CLASS_NAME) == null) {
                  injector.registerBinding(ContextSelector.KEY, () -> new BundleContextSelector(injector));
               }
            }

            public int getOrder() {
               return -50;
            }
         },
         new Hashtable()
      );
      this.injectorCallback = (InjectorCallback)context.getService(this.injectorCallbackServiceRegistration.getReference());
      ServiceRegistry registry = ServiceRegistry.getInstance();
      long bundleId = bundle.getBundleId();
      registry.registerBundleServices(InjectorCallback.class, bundleId, List.of(this.injectorCallback));
      registry.loadServicesFromBundle(PluginService.class, bundleId, classLoader);
      registry.loadServicesFromBundle(ContextDataProvider.class, bundleId, classLoader);
      registry.loadServicesFromBundle(InjectorCallback.class, bundleId, classLoader);
      this.contextRef.compareAndSet(null, context);
   }

   public void stop(final BundleContext context) throws Exception {
      if (this.injectorCallback != null) {
         this.injectorCallback = null;
         this.injectorCallbackServiceRegistration.unregister();
      }

      if (this.pluginRegistry != null) {
         this.pluginRegistry = null;
         this.pluginRegistryServiceRegistration.unregister();
      }

      this.contextRef.compareAndSet(context, null);
      LogManager.shutdown(false, true);
   }
}
