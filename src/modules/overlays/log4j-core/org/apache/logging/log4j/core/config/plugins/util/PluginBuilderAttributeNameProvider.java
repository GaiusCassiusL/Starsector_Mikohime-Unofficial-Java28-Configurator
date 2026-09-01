package org.apache.logging.log4j.core.config.plugins.util;

import java.util.Optional;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.util.Strings;

public class PluginBuilderAttributeNameProvider implements AnnotatedElementNameProvider<PluginBuilderAttribute> {
   public Optional<String> getSpecifiedName(final PluginBuilderAttribute annotation) {
      return Strings.trimToOptional(annotation.value());
   }
}
