package org.apache.logging.log4j.core.config.jason;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.util.ResolverUtil;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.JsonReader;

public class JsonConfiguration extends AbstractConfiguration implements Reconfigurable {
   private static final String[] VERBOSE_CLASSES = new String[]{ResolverUtil.class.getName()};
   private final List<JsonConfiguration.Status> statuses = new ArrayList<>();
   private Map<String, Object> root;

   public JsonConfiguration(final LoggerContext loggerContext, final ConfigurationSource configurationSource) {
      super(loggerContext, configurationSource);

      try {
         byte[] bytes;
         try (InputStream configStream = configurationSource.getInputStream()) {
            bytes = configStream.readAllBytes();
            this.root = (Map<String, Object>)Cast.cast(JsonReader.read(new String(bytes, StandardCharsets.UTF_8)));
         }

         if (this.root.size() == 1) {
            for (Object value : this.root.values()) {
               this.root = (Map<String, Object>)Cast.cast(value);
            }
         }

         processAttributes(this.rootNode, this.root);
         StatusConfiguration statusConfig = new StatusConfiguration().setVerboseClasses(VERBOSE_CLASSES).setStatus(this.getDefaultStatus());
         AtomicInteger monitorIntervalSeconds = new AtomicInteger();
         this.rootNode.getAttributes().forEach((key, value) -> {
            if ("status".equalsIgnoreCase(key)) {
               statusConfig.setStatus(value);
            } else if ("dest".equalsIgnoreCase(key)) {
               statusConfig.setDestination(value);
            } else if ("shutdownHook".equalsIgnoreCase(key)) {
               this.isShutdownHookEnabled = !"disable".equalsIgnoreCase(value);
            } else if ("shutdownTimeout".equalsIgnoreCase(key)) {
               this.shutdownTimeoutMillis = Long.parseLong(value);
            } else if ("verbose".equalsIgnoreCase(key)) {
               statusConfig.setVerbosity(value);
            } else if ("packages".equalsIgnoreCase(key)) {
               LOGGER.warn("The packages attribute is no longer supported");
            } else if ("name".equalsIgnoreCase(key)) {
               this.setName(value);
            } else if ("monitorInterval".equalsIgnoreCase(key)) {
               monitorIntervalSeconds.setOpaque(Integer.parseInt(value));
            } else if ("advertiser".equalsIgnoreCase(key)) {
               this.createAdvertiser(value, configurationSource, bytes, "application/json");
            }
         });
         this.initializeWatchers(this, configurationSource, monitorIntervalSeconds.getOpaque());
         statusConfig.initialize();
         if (this.getName() == null) {
            this.setName(configurationSource.getLocation());
         }
      } catch (Exception e) {
         LOGGER.error("Error parsing {}", configurationSource.getLocation(), e);
      }
   }

   @Override
   public void setup() {
      List<Node> children = this.rootNode.getChildren();
      this.root.forEach((key, value) -> {
         if (value instanceof Map) {
            LOGGER.debug("Processing node for object {}", key);
            children.add(this.constructNode(key, this.rootNode, (Map<String, Object>)Cast.cast(value)));
         }
      });
      LOGGER.debug("Completed parsing configuration");
      if (this.statuses.size() > 0) {
         for (JsonConfiguration.Status s : this.statuses) {
            LOGGER.error("Error processing element {}: {}", s.name, s.errorType);
         }
      }
   }

   private Node constructNode(final String key, final Node parent, final Map<String, Object> value) {
      PluginType<?> pluginType = this.corePlugins.get(key);
      Node node = new Node(parent, key, pluginType);
      processAttributes(node, value);
      List<Node> children = node.getChildren();
      value.forEach((k, v) -> {
         if (isValueType(v)) {
            LOGGER.debug("Node {} is of type {}", k, v != null ? v.getClass() : null);
         } else if (pluginType == null) {
            this.statuses.add(new JsonConfiguration.Status(v, k, JsonConfiguration.ErrorType.CLASS_NOT_FOUND));
         } else {
            if (v instanceof List) {
               LOGGER.debug("Processing node for array {}", k);
               ((List)v).forEach(object -> {
                  if (object instanceof Map) {
                     Map<String, Object> map = (Map<String, Object>)Cast.cast(object);
                     String type = getType(map).orElse(k);
                     PluginType<?> entryType = this.corePlugins.get(type);
                     Node child = new Node(node, k, entryType);
                     processAttributes(child, map);
                     if (type.equalsIgnoreCase(k)) {
                        LOGGER.debug("Processing {}[{}]", k, children.size());
                     } else {
                        LOGGER.debug("Processing {} {}[{}]", type, k, children.size());
                     }

                     List<Node> grandchildren = child.getChildren();
                     map.forEach((itemKey, itemValue) -> {
                        if (itemValue instanceof Map) {
                           LOGGER.debug("Processing node for object {}", itemKey);
                           grandchildren.add(this.constructNode(itemKey, child, (Map<String, Object>)Cast.cast(itemValue)));
                        } else if (itemValue instanceof List) {
                           List<?> list = (List<?>)itemValue;
                           LOGGER.debug("Processing array for object {}", itemKey);
                           list.forEach(subValue -> grandchildren.add(this.constructNode(itemKey, child, (Map<String, Object>)Cast.cast(subValue))));
                        }
                     });
                     children.add(child);
                  }
               });
            } else {
               LOGGER.debug("Processing node for object {}", k);
               children.add(this.constructNode(k, node, (Map<String, Object>)Cast.cast(v)));
            }
         }
      });
      String t;
      if (pluginType == null) {
         t = "null";
      } else {
         t = pluginType.getElementType() + ":" + pluginType.getPluginClass();
      }

      String p = node.getParent() == null ? "null" : (node.getParent().getName() == null ? "root" : node.getParent().getName());
      LOGGER.debug("Returning {} with parent {} of type {}", node.getName(), p, t);
      return node;
   }

   @Override
   public Configuration reconfigure() {
      try {
         ConfigurationSource configurationSource = this.getConfigurationSource().resetInputStream();
         return configurationSource == null ? null : new JsonConfiguration(this.getLoggerContext(), configurationSource);
      } catch (IOException e) {
         LOGGER.error("Cannot locate file {}", this.getConfigurationSource(), e);
         return null;
      }
   }

   private static boolean isValueType(final Object value) {
      return !(value instanceof Map) && !(value instanceof List);
   }

   private static void processAttributes(final Node parent, final Map<String, Object> node) {
      Map<String, String> attributes = parent.getAttributes();
      node.forEach((key, value) -> {
         if (!key.equalsIgnoreCase("type") && isValueType(value)) {
            attributes.put(key, String.valueOf(value));
         }
      });
   }

   private static Optional<String> getType(final Map<String, Object> node) {
      for (Entry<String, Object> entry : node.entrySet()) {
         if (entry.getKey().equalsIgnoreCase("type")) {
            Object value = entry.getValue();
            if (isValueType(value)) {
               return Optional.of(String.valueOf(value));
            }
         }
      }

      return Optional.empty();
   }

   private enum ErrorType {
      CLASS_NOT_FOUND;
   }

   private static final class Status {
      private final Object node;
      private final String name;
      private final JsonConfiguration.ErrorType errorType;

      private Status(final Object node, final String name, final JsonConfiguration.ErrorType errorType) {
         this.node = node;
         this.name = name;
         this.errorType = errorType;
      }

      @Override
      public String toString() {
         return "Status{node=" + this.node + ", name='" + this.name + "', errorType=" + this.errorType + "}";
      }
   }
}
