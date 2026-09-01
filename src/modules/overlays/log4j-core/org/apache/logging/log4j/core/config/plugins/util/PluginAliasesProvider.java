package org.apache.logging.log4j.core.config.plugins.util;

import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.name.AnnotatedElementAliasesProvider;

public class PluginAliasesProvider implements AnnotatedElementAliasesProvider<PluginAliases> {
   public Collection<String> getAliases(final PluginAliases annotation) {
      return List.of(annotation.value());
   }
}
