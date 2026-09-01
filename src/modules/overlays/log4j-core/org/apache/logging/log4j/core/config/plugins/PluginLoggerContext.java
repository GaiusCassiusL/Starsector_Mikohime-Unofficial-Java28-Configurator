package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.logging.log4j.core.config.plugins.visit.PluginLoggerContextVisitor;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.visit.NodeVisitor.Kind;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Kind(PluginLoggerContextVisitor.class)
@QualifierType
public @interface PluginLoggerContext {
}
