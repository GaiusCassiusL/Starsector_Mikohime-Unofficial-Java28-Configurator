package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.logging.log4j.core.config.plugins.util.PluginAttributeNameProvider;
import org.apache.logging.log4j.core.config.plugins.visit.PluginAttributeVisitor;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.visit.NodeVisitor.Kind;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Kind(PluginAttributeVisitor.class)
@NameProvider(PluginAttributeNameProvider.class)
@QualifierType
@Deprecated(since = "3.0.0")
public @interface PluginAttribute {
   boolean defaultBoolean() default false;

   byte defaultByte() default 0;

   char defaultChar() default '\u0000';

   Class<?> defaultClass() default Object.class;

   double defaultDouble() default 0.0;

   float defaultFloat() default 0.0F;

   int defaultInt() default 0;

   long defaultLong() default 0L;

   short defaultShort() default 0;

   String defaultString() default "";

   String value();

   boolean sensitive() default false;
}
