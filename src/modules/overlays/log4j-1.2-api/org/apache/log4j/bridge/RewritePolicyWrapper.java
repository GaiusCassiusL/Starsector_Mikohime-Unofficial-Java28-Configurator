package org.apache.log4j.bridge;

import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;

public class RewritePolicyWrapper implements RewritePolicy {
   private final org.apache.logging.log4j.core.appender.rewrite.RewritePolicy policy;

   public RewritePolicyWrapper(final org.apache.logging.log4j.core.appender.rewrite.RewritePolicy policy) {
      this.policy = policy;
   }

   @Override
   public LoggingEvent rewrite(final LoggingEvent source) {
      LogEvent event = source instanceof LogEventAdapter ? ((LogEventAdapter)source).getEvent() : new LogEventWrapper(source);
      return new LogEventAdapter(this.policy.rewrite(event));
   }

   public org.apache.logging.log4j.core.appender.rewrite.RewritePolicy getPolicy() {
      return this.policy;
   }
}
