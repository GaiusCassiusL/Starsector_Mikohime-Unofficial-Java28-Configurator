package org.apache.logging.log4j.core.config.json;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.util.ResolverUtil;

public class JsonConfiguration extends AbstractConfiguration implements Reconfigurable {
   private static final String[] VERBOSE_CLASSES = new String[]{ResolverUtil.class.getName()};
   private final List<JsonConfiguration.Status> status = new ArrayList<>();
   private JsonNode root;

   public JsonConfiguration(final LoggerContext loggerContext, final ConfigurationSource configSource) {
      super(loggerContext, configSource);

      try {
         byte[] buffer;
         try (InputStream configStream = configSource.getInputStream()) {
            buffer = configStream.readAllBytes();
         }

         InputStream is = new ByteArrayInputStream(buffer);
         this.root = this.getObjectMapper().readTree(is);
         if (this.root.size() == 1) {
            for (JsonNode node : this.root) {
               this.root = node;
            }
         }

         this.processAttributes(this.rootNode, this.root);
         StatusConfiguration statusConfig = new StatusConfiguration().setVerboseClasses(VERBOSE_CLASSES).setStatus(this.getDefaultStatus());
         int monitorIntervalSeconds = 0;

         for (Entry<String, String> entry : this.rootNode.getAttributes().entrySet()) {
            String key = entry.getKey();
            String value = this.getConfigurationStrSubstitutor().replace(entry.getValue());
            if ("status".equalsIgnoreCase(key)) {
               statusConfig.setStatus(value);
            } else if ("dest".equalsIgnoreCase(key)) {
               statusConfig.setDestination(value);
            } else if ("shutdownHook".equalsIgnoreCase(key)) {
               this.isShutdownHookEnabled = !"disable".equalsIgnoreCase(value);
            } else if ("shutdownTimeout".equalsIgnoreCase(key)) {
               this.shutdownTimeoutMillis = Long.parseLong(value);
            } else if ("verbose".equalsIgnoreCase(entry.getKey())) {
               statusConfig.setVerbosity(value);
            } else if ("packages".equalsIgnoreCase(key)) {
               LOGGER.warn("The packages attribute is no longer supported");
            } else if ("name".equalsIgnoreCase(key)) {
               this.setName(value);
            } else if ("monitorInterval".equalsIgnoreCase(key)) {
               monitorIntervalSeconds = Integer.parseInt(value);
            } else if ("advertiser".equalsIgnoreCase(key)) {
               this.createAdvertiser(value, configSource, buffer, "application/json");
            }
         }

         this.initializeWatchers(this, configSource, monitorIntervalSeconds);
         statusConfig.initialize();
         if (this.getName() == null) {
            this.setName(configSource.getLocation());
         }
      } catch (Exception ex) {
         LOGGER.error("Error parsing " + configSource.getLocation(), ex);
      }
   }

   protected ObjectMapper getObjectMapper() {
      return new ObjectMapper().configure(Feature.ALLOW_COMMENTS, true);
   }

   @Override
   public void setup() {
      Iterator<Entry<String, JsonNode>> iter = this.root.fields();
      List<Node> children = this.rootNode.getChildren();

      while (iter.hasNext()) {
         Entry<String, JsonNode> entry = iter.next();
         JsonNode n = entry.getValue();
         if (n.isObject()) {
            LOGGER.debug("Processing node for object {}", entry.getKey());
            children.add(this.constructNode(entry.getKey(), this.rootNode, n));
         } else if (n.isArray()) {
            LOGGER.error("Arrays are not supported at the root configuration.");
         }
      }

      LOGGER.debug("Completed parsing configuration");
      if (this.status.size() > 0) {
         for (JsonConfiguration.Status s : this.status) {
            LOGGER.error("Error processing element {}: {}", s.name, s.errorType);
         }
      }
   }

   @Override
   public Configuration reconfigure() {
      try {
         ConfigurationSource source = this.getConfigurationSource().resetInputStream();
         return source == null ? null : new JsonConfiguration(this.getLoggerContext(), source);
      } catch (IOException ex) {
         LOGGER.error("Cannot locate file {}", this.getConfigurationSource(), ex);
         return null;
      }
   }

   private Node constructNode(final String name, final Node parent, final JsonNode jsonNode) {
      PluginType<?> type = this.corePlugins.get(name);
      Node node = new Node(parent, name, type);
      this.processAttributes(node, jsonNode);
      Iterator<Entry<String, JsonNode>> iter = jsonNode.fields();
      List<Node> children = node.getChildren();

      while (iter.hasNext()) {
         Entry<String, JsonNode> entry = iter.next();
         JsonNode n = entry.getValue();
         if (!n.isArray() && !n.isObject()) {
            LOGGER.debug("Node {} is of type {}", entry.getKey(), n.getNodeType());
         } else {
            if (type == null) {
               this.status.add(new JsonConfiguration.Status(name, n, JsonConfiguration.ErrorType.CLASS_NOT_FOUND));
            }

            if (!n.isArray()) {
               LOGGER.debug("Processing node for object {}", entry.getKey());
               children.add(this.constructNode(entry.getKey(), node, n));
            } else {
               LOGGER.debug("Processing node for array {}", entry.getKey());

               for (int i = 0; i < n.size(); i++) {
                  String pluginType = this.getType(n.get(i), entry.getKey());
                  PluginType<?> entryType = this.corePlugins.get(pluginType);
                  Node item = new Node(node, entry.getKey(), entryType);
                  this.processAttributes(item, n.get(i));
                  if (pluginType.equals(entry.getKey())) {
                     LOGGER.debug("Processing {}[{}]", entry.getKey(), i);
                  } else {
                     LOGGER.debug("Processing {} {}[{}]", pluginType, entry.getKey(), i);
                  }

                  Iterator<Entry<String, JsonNode>> itemIter = n.get(i).fields();
                  List<Node> itemChildren = item.getChildren();

                  while (itemIter.hasNext()) {
                     Entry<String, JsonNode> itemEntry = itemIter.next();
                     if (itemEntry.getValue().isObject()) {
                        LOGGER.debug("Processing node for object {}", itemEntry.getKey());
                        itemChildren.add(this.constructNode(itemEntry.getKey(), item, itemEntry.getValue()));
                     } else if (itemEntry.getValue().isArray()) {
                        JsonNode array = itemEntry.getValue();
                        String entryName = itemEntry.getKey();
                        LOGGER.debug("Processing array for object {}", entryName);

                        for (int j = 0; j < array.size(); j++) {
                           itemChildren.add(this.constructNode(entryName, item, array.get(j)));
                        }
                     }
                  }

                  children.add(item);
               }
            }
         }
      }

      String t;
      if (type == null) {
         t = "null";
      } else {
         t = type.getElementType() + ":" + type.getPluginClass();
      }

      String p = node.getParent() == null ? "null" : (node.getParent().getName() == null ? "root" : node.getParent().getName());
      LOGGER.debug("Returning {} with parent {} of type {}", node.getName(), p, t);
      return node;
   }

   private String getType(final JsonNode node, final String name) {
      Iterator<Entry<String, JsonNode>> iter = node.fields();

      while (iter.hasNext()) {
         Entry<String, JsonNode> entry = iter.next();
         if (entry.getKey().equalsIgnoreCase("type")) {
            JsonNode n = entry.getValue();
            if (n.isValueNode()) {
               return n.asText();
            }
         }
      }

      return name;
   }

   private void processAttributes(final Node parent, final JsonNode node) {
      Map<String, String> attrs = parent.getAttributes();
      Iterator<Entry<String, JsonNode>> iter = node.fields();

      while (iter.hasNext()) {
         Entry<String, JsonNode> entry = iter.next();
         if (!entry.getKey().equalsIgnoreCase("type")) {
            JsonNode n = entry.getValue();
            if (n.isValueNode()) {
               attrs.put(entry.getKey(), n.asText());
            }
         }
      }
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "[location=" + this.getConfigurationSource() + "]";
   }

   private enum ErrorType {
      CLASS_NOT_FOUND;
   }

   private static class Status {
      private final JsonNode node;
      private final String name;
      private final JsonConfiguration.ErrorType errorType;

      public Status(final String name, final JsonNode node, final JsonConfiguration.ErrorType errorType) {
         this.name = name;
         this.node = node;
         this.errorType = errorType;
      }

      @Override
      public String toString() {
         return "Status [name=" + this.name + ", errorType=" + this.errorType + ", node=" + this.node + "]";
      }
   }
}
