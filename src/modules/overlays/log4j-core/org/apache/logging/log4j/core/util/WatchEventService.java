package org.apache.logging.log4j.core.util;

public interface WatchEventService {
   void subscribe(WatchManager manager);

   void unsubscribe(WatchManager manager);
}
