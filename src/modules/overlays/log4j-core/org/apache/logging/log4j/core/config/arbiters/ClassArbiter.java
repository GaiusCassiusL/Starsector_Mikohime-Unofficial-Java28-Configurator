package org.apache.logging.log4j.core.config.arbiters;

import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.LoaderUtil;

@Configurable(elementType = "Arbiter", printObject = true, deferChildren = true)
@Plugin
public final class ClassArbiter implements Arbiter {
   private final String className;

   private ClassArbiter(final String className) {
      this.className = className;
   }

   @Override
   public boolean isCondition() {
      return LoaderUtil.isClassAvailable(this.className);
   }

   @PluginBuilderFactory
   public static ClassArbiter.Builder newBuilder() {
      return new ClassArbiter.Builder();
   }

   public static class Builder implements org.apache.logging.log4j.core.util.Builder<ClassArbiter> {
      public static final String ATTR_CLASS_NAME = "className";
      @PluginBuilderAttribute("className")
      private String className;

      public ClassArbiter.Builder setClassName(final String className) {
         this.className = className;
         return this.asBuilder();
      }

      public ClassArbiter.Builder asBuilder() {
         return this;
      }

      public ClassArbiter build() {
         return new ClassArbiter(this.className);
      }
   }
}
