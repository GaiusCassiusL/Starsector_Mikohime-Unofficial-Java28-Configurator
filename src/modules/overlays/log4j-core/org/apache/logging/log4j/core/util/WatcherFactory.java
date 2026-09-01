package org.apache.logging.log4j.core.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFileWatcher;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.status.StatusLogger;

public class WatcherFactory {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final PluginNamespace plugins;

   public WatcherFactory(final PluginNamespace watcherPlugins) {
      this.plugins = watcherPlugins;
   }

   public Watcher newWatcher(
      final Source source,
      final Configuration configuration,
      final Reconfigurable reconfigurable,
      final List<ConfigurationListener> configurationListeners,
      final long lastModifiedMillis
   ) {
      if (source.getFile() != null) {
         return new ConfigurationFileWatcher(configuration, reconfigurable, configurationListeners, lastModifiedMillis);
      }

      String name = source.getURI().getScheme();
      PluginType<?> pluginType = this.plugins.get(name);
      if (pluginType != null) {
         return instantiate(
            name, pluginType.getPluginClass().asSubclass(Watcher.class), configuration, reconfigurable, configurationListeners, lastModifiedMillis
         );
      }

      LOGGER.info("No Watcher plugin is available for protocol '{}'", name);
      return null;
   }

   public static <T extends Watcher> T instantiate(
      final String name,
      final Class<T> clazz,
      final Configuration configuration,
      final Reconfigurable reconfigurable,
      final List<ConfigurationListener> listeners,
      final long lastModifiedMillis
   ) {
      Objects.requireNonNull(clazz, "No class provided");

      try {
         Constructor<T> constructor = clazz.getConstructor(Configuration.class, Reconfigurable.class, List.class, long.class);
         return constructor.newInstance(configuration, reconfigurable, listeners, lastModifiedMillis);
      } catch (NoSuchMethodException ex) {
         throw new IllegalArgumentException("No valid constructor for Watcher plugin " + name, ex);
      } catch (LinkageError | InstantiationException e) {
         throw new IllegalArgumentException(e);
      } catch (IllegalAccessException e) {
         throw new IllegalStateException(e);
      } catch (InvocationTargetException e) {
         Throwables.rethrow(e.getCause());
         throw new InternalError("Unreachable");
      }
   }
}
