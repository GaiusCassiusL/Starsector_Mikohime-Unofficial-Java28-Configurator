package org.apache.logging.log4j.core.config.plugins.util;

import java.util.Optional;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.util.Strings;

public class PluginElementNameProvider implements AnnotatedElementNameProvider<PluginElement> {
   public Optional<String> getSpecifiedName(final PluginElement annotation) {
      return Strings.trimToOptional(annotation.value());
   }
}
