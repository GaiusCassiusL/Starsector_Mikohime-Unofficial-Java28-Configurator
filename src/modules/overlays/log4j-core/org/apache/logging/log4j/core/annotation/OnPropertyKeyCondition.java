package org.apache.logging.log4j.core.annotation;

import java.lang.reflect.AnnotatedElement;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.condition.Condition;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyKey;

@Singleton
public class OnPropertyKeyCondition implements Condition {
   private static final Logger LOGGER = StatusLogger.getLogger();

   public boolean matches(final Key<?> key, final AnnotatedElement element) {
      ConditionalOnPropertyKey annotation = (ConditionalOnPropertyKey)AnnotationUtil.getLogicalAnnotation(element, ConditionalOnPropertyKey.class);
      if (annotation == null) {
         return false;
      }

      PropertyKey propertyKey = annotation.key();
      String value = annotation.value();
      String property = PropertiesUtil.getProperties().getStringProperty(propertyKey);
      boolean result = property != null && (value.isEmpty() || value.equalsIgnoreCase(property));
      LOGGER.debug("ConditionalOnPropertyKey {} for key='{}', value='{}'; property='{}'", result, propertyKey.getName(), value, property);
      return result;
   }
}
