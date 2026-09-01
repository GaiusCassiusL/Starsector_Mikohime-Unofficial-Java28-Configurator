package org.apache.logging.log4j.core;

import java.util.concurrent.TimeUnit;

public interface LifeCycle {
   LifeCycle.State getState();

   void initialize();

   void start();

   void stop();

   boolean isStarted();

   boolean isStopped();

   boolean stop(long timeout, TimeUnit timeUnit);

   enum State {
      INITIALIZING,
      INITIALIZED,
      STARTING,
      STARTED,
      STOPPING,
      STOPPED;
   }
}
