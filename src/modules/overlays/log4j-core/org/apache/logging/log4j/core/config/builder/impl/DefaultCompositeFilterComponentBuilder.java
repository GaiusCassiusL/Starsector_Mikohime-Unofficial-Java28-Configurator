package org.apache.logging.log4j.core.config.builder.impl;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.CompositeFilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;

class DefaultCompositeFilterComponentBuilder
   extends DefaultComponentAndConfigurationBuilder<CompositeFilterComponentBuilder>
   implements CompositeFilterComponentBuilder {
   public DefaultCompositeFilterComponentBuilder(
      final DefaultConfigurationBuilder<? extends Configuration> builder, final String onMatch, final String onMismatch
   ) {
      super(builder, "Filters");
      this.addAttribute("onMatch", onMatch);
      this.addAttribute("onMismatch", onMismatch);
   }

   public CompositeFilterComponentBuilder add(final FilterComponentBuilder builder) {
      return this.addComponent(builder);
   }
}
