package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilderAttributeNameProvider;
import org.apache.logging.log4j.core.config.plugins.visit.PluginBuilderAttributeVisitor;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.visit.NodeVisitor.Kind;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Kind(PluginBuilderAttributeVisitor.class)
@NameProvider(PluginBuilderAttributeNameProvider.class)
@QualifierType
@Deprecated(since = "3.0.0")
public @interface PluginBuilderAttribute {
   String value() default "";

   boolean sensitive() default false;
}
