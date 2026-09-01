package org.apache.logging.log4j.core.config.plugins.util;

import java.util.Optional;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.util.Strings;

public class PluginAttributeNameProvider implements AnnotatedElementNameProvider<PluginAttribute> {
   public Optional<String> getSpecifiedName(final PluginAttribute annotation) {
      return Strings.trimToOptional(annotation.value());
   }
}
