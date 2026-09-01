package org.apache.logging.log4j.core.appender.routing;

import org.apache.logging.log4j.core.LogEvent;

public interface PurgePolicy {
   void purge();

   void update(String key, LogEvent event);

   void initialize(RoutingAppender routingAppender);
}
