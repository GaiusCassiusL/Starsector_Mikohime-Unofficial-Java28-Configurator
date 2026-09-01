package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginNamespace;

public interface StrLookup {
   String CATEGORY = "Lookup";
   Key<PluginNamespace> PLUGIN_CATEGORY_KEY = new Key<PluginNamespace>() {};

   String lookup(String key);

   String lookup(LogEvent event, String key);
}
