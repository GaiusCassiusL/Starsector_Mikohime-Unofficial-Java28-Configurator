package org.apache.logging.log4j.core.config.plugins.visit;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.visit.NodeVisitor;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.StringBuilders;

public class PluginConfigurationVisitor implements NodeVisitor {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final Configuration configuration;

   @Inject
   public PluginConfigurationVisitor(final Configuration configuration) {
      this.configuration = configuration;
   }

   public Object visitField(final Field field, final Node node, final StringBuilder debugLog) {
      if (TypeUtil.isAssignable(field.getGenericType(), this.configuration.getClass())) {
         StringBuilders.appendKeyDqValueWithJoiner(debugLog, "configuration", this.configuration, ", ");
         return Cast.cast(this.configuration);
      } else {
         LOGGER.error("Field {} annotated with @PluginConfiguration is not compatible with type {}", field, this.configuration.getClass());
         return null;
      }
   }

   public Object visitParameter(final Parameter parameter, final Node node, final StringBuilder debugLog) {
      if (TypeUtil.isAssignable(parameter.getParameterizedType(), this.configuration.getClass())) {
         StringBuilders.appendKeyDqValueWithJoiner(debugLog, "configuration", this.configuration, ", ");
         return Cast.cast(this.configuration);
      } else {
         LOGGER.error("Parameter {} annotated with @PluginConfiguration is not compatible with type {}", parameter, this.configuration.getClass());
         return null;
      }
   }
}
