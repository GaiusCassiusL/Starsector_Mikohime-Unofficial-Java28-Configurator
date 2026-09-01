package org.apache.logging.log4j.core.appender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

@Configurable(elementType = "appender", printObject = true)
@Plugin("Failover")
public final class FailoverAppender extends AbstractAppender {
   private static final int DEFAULT_INTERVAL_SECONDS = 60;
   private final String primaryRef;
   private final String[] failovers;
   private final Configuration config;
   private AppenderControl primary;
   private final List<AppenderControl> failoverAppenders = new ArrayList<>();
   private final long intervalNanos;
   private volatile long nextCheckNanos;

   private FailoverAppender(
      final String name,
      final Filter filter,
      final String primary,
      final String[] failovers,
      final int intervalMillis,
      final Configuration config,
      final boolean ignoreExceptions,
      final Property[] properties
   ) {
      super(name, filter, null, ignoreExceptions, properties);
      this.primaryRef = primary;
      this.failovers = failovers;
      this.config = config;
      this.intervalNanos = TimeUnit.MILLISECONDS.toNanos(intervalMillis);
   }

   @Override
   public void start() {
      Map<String, Appender> map = this.config.getAppenders();
      int errors = 0;
      Appender appender = map.get(this.primaryRef);
      if (appender != null) {
         this.primary = new AppenderControl(appender, null, null);
      } else {
         LOGGER.error("Unable to locate primary Appender " + this.primaryRef);
         errors++;
      }

      for (String name : this.failovers) {
         Appender foAppender = map.get(name);
         if (foAppender != null) {
            this.failoverAppenders.add(new AppenderControl(foAppender, null, null));
         } else {
            LOGGER.error("Failover appender " + name + " is not configured");
         }
      }

      if (this.failoverAppenders.isEmpty()) {
         LOGGER.error("No failover appenders are available");
         errors++;
      }

      if (errors == 0) {
         super.start();
      }
   }

   @Override
   public void append(final LogEvent event) {
      if (!this.isStarted()) {
         this.error("FailoverAppender " + this.getName() + " did not start successfully");
      } else {
         long localCheckNanos = this.nextCheckNanos;
         if (localCheckNanos != 0L && System.nanoTime() - localCheckNanos <= 0L) {
            this.failover(event, null);
         } else {
            this.callAppender(event);
         }
      }
   }

   private void callAppender(final LogEvent event) {
      try {
         this.primary.callAppender(event);
         this.nextCheckNanos = 0L;
      } catch (Exception ex) {
         this.nextCheckNanos = System.nanoTime() + this.intervalNanos;
         this.failover(event, ex);
      }
   }

   private void failover(final LogEvent event, final Exception ex) {
      RuntimeException re = ex != null ? (ex instanceof LoggingException ? (LoggingException)ex : new LoggingException(ex)) : null;
      boolean written = false;
      Exception failoverException = null;

      for (AppenderControl control : this.failoverAppenders) {
         try {
            control.callAppender(event);
            written = true;
            break;
         } catch (Exception fex) {
            if (failoverException == null) {
               failoverException = fex;
            }
         }
      }

      if (!written && !this.ignoreExceptions()) {
         if (re != null) {
            throw re;
         } else {
            throw new LoggingException("Unable to write to failover appenders", failoverException);
         }
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(this.getName());
      sb.append(" primary=").append(this.primary).append(", failover={");
      boolean first = true;

      for (String str : this.failovers) {
         if (!first) {
            sb.append(", ");
         }

         sb.append(str);
         first = false;
      }

      sb.append('}');
      return sb.toString();
   }

   @PluginFactory
   public static FailoverAppender createAppender(
      @PluginAttribute @Required(message = "A name for the Appender must be specified") final String name,
      @PluginAttribute @Required(message = "A primary Appender must be specified") final String primary,
      @PluginElement @Required(message = "At least one failover Appender must be specified") final String[] failovers,
      @PluginAliases("retryInterval") @PluginAttribute(defaultInt = 60) final int retryIntervalSeconds,
      final Configuration config,
      @PluginElement final Filter filter,
      @PluginAttribute(defaultBoolean = true) final boolean ignoreExceptions
   ) {
      int retryIntervalMillis;
      if (retryIntervalSeconds >= 0) {
         retryIntervalMillis = retryIntervalSeconds * 1000;
      } else {
         LOGGER.warn("Interval {} is less than zero. Using default", retryIntervalSeconds);
         retryIntervalMillis = 60000;
      }

      return new FailoverAppender(name, filter, primary, failovers, retryIntervalMillis, config, ignoreExceptions, Property.EMPTY_ARRAY);
   }
}
