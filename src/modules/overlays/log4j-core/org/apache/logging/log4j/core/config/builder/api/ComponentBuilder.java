package org.apache.logging.log4j.core.config.builder.api;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.plugins.util.Builder;

public interface ComponentBuilder<T extends ComponentBuilder<T>> extends Builder<Component> {
   T addAttribute(String key, String value);

   T addAttribute(String key, Level level);

   T addAttribute(String key, Enum<?> value);

   T addAttribute(String key, int value);

   T addAttribute(String key, boolean value);

   T addAttribute(String key, Object value);

   T addComponent(ComponentBuilder<?> builder);

   String getName();

   ConfigurationBuilder<? extends Configuration> getBuilder();
}
