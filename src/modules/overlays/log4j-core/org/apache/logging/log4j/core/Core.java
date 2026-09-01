package org.apache.logging.log4j.core;

import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginNamespace;

public class Core {
   @Deprecated
   public static final String CATEGORY_NAME = "Core";
   public static final Key<PluginNamespace> PLUGIN_NAMESPACE_KEY = new Key<PluginNamespace>() {};
}
