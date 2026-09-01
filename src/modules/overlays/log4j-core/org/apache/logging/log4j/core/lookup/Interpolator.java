package org.apache.logging.log4j.core.lookup;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationAware;
import org.apache.logging.log4j.core.config.LoggerContextAware;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.status.StatusLogger;

public class Interpolator extends AbstractConfigurationAwareLookup implements LoggerContextAware {
   public static final char PREFIX_SEPARATOR = ':';
   private static final String LOOKUP_KEY_WEB = "web";
   private static final String LOOKUP_KEY_DOCKER = "docker";
   private static final String LOOKUP_KEY_KUBERNETES = "kubernetes";
   private static final String LOOKUP_KEY_SPRING = "spring";
   private static final String LOOKUP_KEY_JNDI = "jndi";
   private static final String LOOKUP_KEY_JVMRUNARGS = "jvmrunargs";
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final Map<String, Supplier<? extends StrLookup>> strLookups = new ConcurrentHashMap<>();
   private final StrLookup defaultLookup;
   private WeakReference<LoggerContext> loggerContext = null;

   public Interpolator(final StrLookup defaultLookup) {
      this.defaultLookup = defaultLookup == null ? new PropertiesLookup(Map.of()) : defaultLookup;
      Injector injector = DI.createInjector();
      ((PluginNamespace)injector.getInstance(PLUGIN_CATEGORY_KEY)).forEach((key, value) -> {
         try {
            this.strLookups.put(key, injector.getFactory(value.getPluginClass().asSubclass(StrLookup.class)));
         } catch (Throwable t) {
            this.handleError(key, t);
         }
      });
   }

   public Interpolator(final StrLookup defaultLookup, final Map<String, Supplier<StrLookup>> strLookupPlugins) {
      this.defaultLookup = defaultLookup;
      this.strLookups.putAll(strLookupPlugins);
   }

   public Interpolator() {
      this(Map.of());
   }

   public Interpolator(final Map<String, String> properties) {
      this(new PropertiesLookup(properties));
   }

   public StrLookup getDefaultLookup() {
      return this.defaultLookup;
   }

   public Map<String, StrLookup> getStrLookupMap() {
      return this.strLookups.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().get()));
   }

   private void handleError(final String lookupKey, final Throwable t) {
      switch (lookupKey) {
         case "jndi":
            LOGGER.warn(
               "JNDI lookup class is not available because this JRE does not support JNDI. JNDI string lookups will not be available, continuing configuration. Ignoring "
                  + t
            );
            break;
         case "jvmrunargs":
            LOGGER.warn(
               "JMX runtime input lookup class is not available because this JRE does not support JMX. JMX lookups will not be available, continuing configuration. Ignoring "
                  + t
            );
            break;
         case "web":
            LOGGER.info(
               "Log4j appears to be running in a Servlet environment, but there's no log4j-web module available. If you want better web container support, please add the log4j-web JAR to your web archive or server lib directory."
            );
         case "docker":
         case "spring":
            break;
         case "kubernetes":
            if (t instanceof NoClassDefFoundError) {
               LOGGER.warn("Unable to create Kubernetes lookup due to missing dependency: {}", t.getMessage());
            }
            break;
         default:
            LOGGER.error("Unable to create Lookup for {}", lookupKey, t);
      }
   }

   @Override
   public String lookup(final LogEvent event, String var) {
      if (var == null) {
         return null;
      }

      int prefixPos = var.indexOf(58);
      if (prefixPos >= 0) {
         String prefix = var.substring(0, prefixPos).toLowerCase(Locale.US);
         String name = var.substring(prefixPos + 1);
         Supplier<? extends StrLookup> lookupSupplier = this.strLookups.get(prefix);
         String value = null;
         if (lookupSupplier != null) {
            StrLookup lookup = lookupSupplier.get();
            if (lookup instanceof ConfigurationAware) {
               ((ConfigurationAware)lookup).setConfiguration(this.configuration);
            }

            if (lookup instanceof LoggerContextAware) {
               ((LoggerContextAware)lookup).setLoggerContext(this.loggerContext.get());
            }

            value = event == null ? lookup.lookup(name) : lookup.lookup(event, name);
         }

         if (value != null) {
            return value;
         }

         var = var.substring(prefixPos + 1);
      }

      if (this.defaultLookup != null) {
         return event == null ? this.defaultLookup.lookup(var) : this.defaultLookup.lookup(event, var);
      } else {
         return null;
      }
   }

   @Override
   public void setLoggerContext(final LoggerContext loggerContext) {
      if (loggerContext != null) {
         this.loggerContext = new WeakReference<>(loggerContext);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();

      for (String name : this.strLookups.keySet()) {
         if (sb.length() == 0) {
            sb.append('{');
         } else {
            sb.append(", ");
         }

         sb.append(name);
      }

      if (sb.length() > 0) {
         sb.append('}');
      }

      return sb.toString();
   }
}
