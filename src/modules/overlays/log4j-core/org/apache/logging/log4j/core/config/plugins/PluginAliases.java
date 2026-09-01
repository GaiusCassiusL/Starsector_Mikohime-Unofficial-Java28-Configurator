package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.logging.log4j.core.config.plugins.util.PluginAliasesProvider;
import org.apache.logging.log4j.plugins.name.AliasesProvider;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.FIELD})
@AliasesProvider(PluginAliasesProvider.class)
@Deprecated(since = "3.0.0")
public @interface PluginAliases {
   String[] value();
}
