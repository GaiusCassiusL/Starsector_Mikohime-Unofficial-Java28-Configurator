package org.apache.logging.log4j.core.config.builder.impl;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;

class DefaultAppenderComponentBuilder extends DefaultComponentAndConfigurationBuilder<AppenderComponentBuilder> implements AppenderComponentBuilder {
   public DefaultAppenderComponentBuilder(final DefaultConfigurationBuilder<? extends Configuration> builder, final String name, final String type) {
      super(builder, name, type);
   }

   @Override
   public AppenderComponentBuilder add(final LayoutComponentBuilder builder) {
      return this.addComponent(builder);
   }

   public AppenderComponentBuilder add(final FilterComponentBuilder builder) {
      return this.addComponent(builder);
   }
}
