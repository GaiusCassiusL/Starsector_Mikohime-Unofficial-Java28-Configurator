package org.apache.logging.log4j.core.config.composite;

import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginNamespace;

public interface MergeStrategy {
   Key<MergeStrategy> KEY = new Key<MergeStrategy>() {};

   void mergeRootProperties(Node rootNode, AbstractConfiguration configuration);

   void mergeConfigurations(Node target, Node source, PluginNamespace corePlugins);
}
