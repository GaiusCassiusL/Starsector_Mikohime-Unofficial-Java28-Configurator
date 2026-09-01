package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.util.Log4jThread;

public class DefaultAsyncQueueFullPolicy implements AsyncQueueFullPolicy {
   @Override
   public EventRoute getRoute(final long backgroundThreadId, final Level level) {
      Thread currentThread = Thread.currentThread();
      return currentThread.getId() != backgroundThreadId && !(currentThread instanceof Log4jThread) ? EventRoute.ENQUEUE : EventRoute.SYNCHRONOUS;
   }
}
