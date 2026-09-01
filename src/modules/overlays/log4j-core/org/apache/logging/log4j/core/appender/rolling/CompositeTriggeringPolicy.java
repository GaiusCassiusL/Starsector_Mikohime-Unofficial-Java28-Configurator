package org.apache.logging.log4j.core.appender.rolling;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin("Policies")
public final class CompositeTriggeringPolicy extends AbstractTriggeringPolicy {
   private final TriggeringPolicy[] triggeringPolicies;

   private CompositeTriggeringPolicy(final TriggeringPolicy... triggeringPolicies) {
      this.triggeringPolicies = triggeringPolicies;
   }

   public TriggeringPolicy[] getTriggeringPolicies() {
      return this.triggeringPolicies;
   }

   @Override
   public void initialize(final RollingFileManager manager) {
      for (TriggeringPolicy triggeringPolicy : this.triggeringPolicies) {
         LOGGER.debug("Initializing triggering policy {}", triggeringPolicy.toString());
         triggeringPolicy.initialize(manager);
      }
   }

   @Override
   public boolean isTriggeringEvent(final LogEvent event) {
      for (TriggeringPolicy triggeringPolicy : this.triggeringPolicies) {
         if (triggeringPolicy.isTriggeringEvent(event)) {
            return true;
         }
      }

      return false;
   }

   @PluginFactory
   public static CompositeTriggeringPolicy createPolicy(@PluginElement("Policies") final TriggeringPolicy... triggeringPolicy) {
      return new CompositeTriggeringPolicy(triggeringPolicy);
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      this.setStopping();
      boolean stopped = true;

      for (TriggeringPolicy triggeringPolicy : this.triggeringPolicies) {
         stopped &= ((LifeCycle)triggeringPolicy).stop(timeout, timeUnit);
      }

      this.setStopped();
      return stopped;
   }

   @Override
   public String toString() {
      return "CompositeTriggeringPolicy(policies=" + Arrays.toString(this.triggeringPolicies) + ")";
   }
}
