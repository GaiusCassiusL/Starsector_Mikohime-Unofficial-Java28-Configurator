package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.logging.log4j.core.config.plugins.util.PluginValueNameProvider;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.visit.PluginValueVisitor;
import org.apache.logging.log4j.plugins.visit.NodeVisitor.Kind;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Kind(PluginValueVisitor.class)
@NameProvider(PluginValueNameProvider.class)
@QualifierType
@Deprecated(since = "3.0.0")
public @interface PluginValue {
   String value();
}
