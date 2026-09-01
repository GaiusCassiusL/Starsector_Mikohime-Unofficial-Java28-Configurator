package org.apache.logging.log4j.core.config.arbiters;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;

@Configurable(elementType = "Arbiter", printObject = true, deferChildren = true)
@Plugin("Select")
public class SelectArbiter {
   public Arbiter evaluateConditions(final List<Arbiter> conditions) {
      Optional<Arbiter> opt = conditions.stream().filter(c -> c instanceof DefaultArbiter).reduce((a, b) -> {
         throw new IllegalStateException("Multiple elements: " + a + ", " + b);
      });

      for (Arbiter condition : conditions) {
         if (!(condition instanceof DefaultArbiter) && condition.isCondition()) {
            return condition;
         }
      }

      return opt.orElse(null);
   }

   @PluginBuilderFactory
   public static SelectArbiter.Builder newBuilder() {
      return new SelectArbiter.Builder();
   }

   public static class Builder implements org.apache.logging.log4j.core.util.Builder<SelectArbiter> {
      public SelectArbiter.Builder asBuilder() {
         return this;
      }

      public SelectArbiter build() {
         return new SelectArbiter();
      }
   }
}
