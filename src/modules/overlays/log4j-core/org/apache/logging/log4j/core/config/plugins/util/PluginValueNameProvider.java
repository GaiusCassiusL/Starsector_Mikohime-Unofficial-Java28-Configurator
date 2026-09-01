package org.apache.logging.log4j.core.config.plugins.util;

import java.util.Optional;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.util.Strings;

public class PluginValueNameProvider implements AnnotatedElementNameProvider<PluginValue> {
   public Optional<String> getSpecifiedName(final PluginValue annotation) {
      return Strings.trimToOptional(annotation.value());
   }
}
