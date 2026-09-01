package org.apache.logging.log4j.core.config.plugins.visit;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.visit.NodeVisitor;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.StringBuilders;

public class PluginLoggerContextVisitor implements NodeVisitor {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final LoggerContext loggerContext;

   @Inject
   public PluginLoggerContextVisitor(final WeakReference<LoggerContext> loggerContext) {
      this.loggerContext = loggerContext.get();
   }

   public Object visitField(final Field field, final Node node, final StringBuilder debugLog) {
      if (TypeUtil.isAssignable(field.getGenericType(), LoggerContext.class)) {
         StringBuilders.appendKeyDqValueWithJoiner(debugLog, "loggerContext", this.loggerContext, ", ");
         return Cast.cast(this.loggerContext);
      } else {
         LOGGER.error("Field {} annotated with @PluginLoggerContext is not compatible with type {}", field, this.loggerContext.getClass());
         return null;
      }
   }

   public Object visitParameter(final Parameter parameter, final Node node, final StringBuilder debugLog) {
      if (TypeUtil.isAssignable(parameter.getParameterizedType(), this.loggerContext.getClass())) {
         StringBuilders.appendKeyDqValueWithJoiner(debugLog, "loggerContext", this.loggerContext, ", ");
         return Cast.cast(this.loggerContext);
      } else {
         LOGGER.error("Parameter {} annotated with @PluginLoggerContext is not compatible with type {}", parameter, this.loggerContext.getClass());
         return null;
      }
   }
}
