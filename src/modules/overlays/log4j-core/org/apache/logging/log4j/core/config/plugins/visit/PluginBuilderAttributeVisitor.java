package org.apache.logging.log4j.core.config.plugins.visit;

import java.lang.reflect.AnnotatedElement;
import java.util.function.Function;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.di.Injector;

public class PluginBuilderAttributeVisitor extends org.apache.logging.log4j.plugins.visit.PluginBuilderAttributeVisitor {
   @Inject
   public PluginBuilderAttributeVisitor(@Named("StringSubstitutor") final Function<String, String> stringSubstitutionStrategy, final Injector injector) {
      super(stringSubstitutionStrategy, injector);
   }

   protected boolean isSensitive(final AnnotatedElement element) {
      return element.getAnnotation(PluginBuilderAttribute.class).sensitive();
   }
}
