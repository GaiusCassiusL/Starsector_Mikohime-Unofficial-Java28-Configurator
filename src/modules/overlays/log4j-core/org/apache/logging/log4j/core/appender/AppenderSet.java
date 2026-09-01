package org.apache.logging.log4j.core.appender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.PluginNode;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(printObject = true, deferChildren = true)
@Plugin
public final class AppenderSet {
   private static final StatusLogger LOGGER = StatusLogger.getLogger();
   private final Configuration configuration;
   private final Map<String, Node> nodeMap;

   @PluginFactory
   public static AppenderSet.Builder newBuilder() {
      return new AppenderSet.Builder();
   }

   private AppenderSet(final Configuration configuration, final Map<String, Node> appenders) {
      this.configuration = configuration;
      this.nodeMap = appenders;
   }

   public Appender createAppender(final String actualAppenderName, final String sourceAppenderName) {
      Node node = this.nodeMap.get(actualAppenderName);
      if (node == null) {
         LOGGER.error("No node named {} in {}", actualAppenderName, this);
         return null;
      }

      node.getAttributes().put("name", sourceAppenderName);
      if (node.getType().getElementType().equals("appender")) {
         Node appNode = new Node(node);
         this.configuration.createConfiguration(appNode, null);
         if (appNode.getObject() instanceof Appender) {
            Appender app = (Appender)appNode.getObject();
            app.start();
            return app;
         } else {
            LOGGER.error("Unable to create Appender of type " + node.getName());
            return null;
         }
      } else {
         LOGGER.error("No Appender was configured for name {} " + actualAppenderName);
         return null;
      }
   }

   public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<AppenderSet> {
      @PluginNode
      private Node node;
      @Inject
      @Required
      private Configuration configuration;

      public AppenderSet build() {
         if (this.configuration == null) {
            AppenderSet.LOGGER.error("Configuration is missing from AppenderSet {}", this);
            return null;
         }

         if (this.node == null) {
            AppenderSet.LOGGER.error("No node in AppenderSet {}", this);
            return null;
         }

         List<Node> children = this.node.getChildren();
         if (children == null) {
            AppenderSet.LOGGER.error("No children node in AppenderSet {}", this);
            return null;
         }

         Map<String, Node> map = new HashMap<>(children.size());

         for (Node childNode : children) {
            String key = (String)childNode.getAttributes().get("name");
            if (key == null) {
               AppenderSet.LOGGER.error("The attribute 'name' is missing from the node {} in AppenderSet {}", childNode, children);
            } else {
               map.put(key, childNode);
            }
         }

         return new AppenderSet(this.configuration, map);
      }

      public Node getNode() {
         return this.node;
      }

      public Configuration getConfiguration() {
         return this.configuration;
      }

      public AppenderSet.Builder setNode(final Node node) {
         this.node = node;
         return this;
      }

      public AppenderSet.Builder setConfiguration(final Configuration configuration) {
         this.configuration = configuration;
         return this;
      }

      @Override
      public String toString() {
         return this.getClass().getName() + " [node=" + this.node + ", configuration=" + this.configuration + "]";
      }
   }
}
