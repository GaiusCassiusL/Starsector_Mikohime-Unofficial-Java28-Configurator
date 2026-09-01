package org.apache.logging.log4j.core.config.arbiters;

import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;

@Configurable(elementType = "Arbiter", printObject = true, deferChildren = true)
@Plugin
public class DefaultArbiter implements Arbiter {
   @Override
   public boolean isCondition() {
      return true;
   }

   @PluginBuilderFactory
   public static DefaultArbiter.Builder newBuilder() {
      return new DefaultArbiter.Builder();
   }

   public static class Builder implements org.apache.logging.log4j.core.util.Builder<DefaultArbiter> {
      public DefaultArbiter.Builder asBuilder() {
         return this;
      }

      public DefaultArbiter build() {
         return new DefaultArbiter();
      }
   }
}
