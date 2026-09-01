package org.apache.log4j.builders;

import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.bridge.RewritePolicyWrapper;
import org.apache.log4j.builders.appender.AppenderBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;
import org.w3c.dom.Element;

public class BuilderManager {
   public static final String NAMESPACE = "Log4j Builder";
   public static final Appender INVALID_APPENDER = new AppenderWrapper(null);
   public static final Filter INVALID_FILTER = new FilterWrapper(null);
   public static final Layout INVALID_LAYOUT = new LayoutWrapper(null);
   public static final RewritePolicy INVALID_REWRITE_POLICY = new RewritePolicyWrapper(null);
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final Class<?>[] CONSTRUCTOR_PARAMS = new Class[]{String.class, Properties.class};
   private final Injector injector;
   private final PluginNamespace plugins;

   @Inject
   public BuilderManager(final Injector injector, @Namespace("Log4j Builder") final PluginNamespace plugins) {
      this.injector = injector;
      this.plugins = plugins;
   }

   private <T extends Builder<U>, U> T createBuilder(final PluginType<T> plugin, final String prefix, final Properties props) {
      if (plugin == null) {
         return null;
      }

      try {
         Class<T> clazz = plugin.getPluginClass();
         if (AbstractBuilder.class.isAssignableFrom(clazz)) {
            return clazz.getConstructor(CONSTRUCTOR_PARAMS).newInstance(prefix, props);
         } else {
            T builder = (T)this.injector.getInstance(clazz);
            if (!Builder.class.isAssignableFrom(clazz)) {
               LOGGER.warn("Unable to load plugin: builder {} does not implement {}", clazz, Builder.class);
               return null;
            } else {
               return builder;
            }
         }
      } catch (ReflectiveOperationException ex) {
         LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
         return null;
      }
   }

   private <T> PluginType<T> getPlugin(final String className) {
      Objects.requireNonNull(this.plugins, "plugins");
      Objects.requireNonNull(className, "className");
      String key = className.toLowerCase(Locale.ROOT).trim();
      PluginType<?> pluginType = this.plugins.get(key);
      if (pluginType == null) {
         LOGGER.warn("Unable to load plugin class name {} with key {}", className, key);
      }

      return (PluginType<T>)Cast.cast(pluginType);
   }

   private <T extends Builder<U>, U> U newInstance(final PluginType<T> plugin, final Function<T, U> consumer, final U invalidValue) {
      if (plugin != null) {
         T builder = (T)this.injector.getInstance(plugin.getPluginClass());
         if (builder != null) {
            U result = consumer.apply(builder);
            return result != null ? result : invalidValue;
         }
      }

      return null;
   }

   public <P extends Parser<T>, T> T parse(
      final String className, final String prefix, final Properties props, final PropertiesConfiguration config, final T invalidValue
   ) {
      P parser = (P)this.createBuilder(this.getPlugin(className), prefix, props);
      if (parser != null) {
         T value = parser.parse(config);
         return value != null ? value : invalidValue;
      } else {
         return null;
      }
   }

   public Appender parseAppender(final String className, final Element appenderElement, final XmlConfiguration config) {
      return this.newInstance(this.getPlugin(className), b -> b.parseAppender(appenderElement, config), INVALID_APPENDER);
   }

   public Appender parseAppender(
      final String name,
      final String className,
      final String prefix,
      final String layoutPrefix,
      final String filterPrefix,
      final Properties props,
      final PropertiesConfiguration config
   ) {
      AppenderBuilder<Appender> builder = this.createBuilder(this.getPlugin(className), prefix, props);
      if (builder != null) {
         Appender appender = builder.parseAppender(name, prefix, layoutPrefix, filterPrefix, props, config);
         return appender != null ? appender : INVALID_APPENDER;
      } else {
         return null;
      }
   }

   public Filter parseFilter(final String className, final Element filterElement, final XmlConfiguration config) {
      return this.newInstance(this.getPlugin(className), b -> b.parse(filterElement, config), INVALID_FILTER);
   }

   public Layout parseLayout(final String className, final Element layoutElement, final XmlConfiguration config) {
      return this.newInstance(this.getPlugin(className), b -> b.parse(layoutElement, config), INVALID_LAYOUT);
   }

   public RewritePolicy parseRewritePolicy(final String className, final Element rewriteElement, final XmlConfiguration config) {
      return this.newInstance(this.getPlugin(className), b -> b.parse(rewriteElement, config), INVALID_REWRITE_POLICY);
   }
}
