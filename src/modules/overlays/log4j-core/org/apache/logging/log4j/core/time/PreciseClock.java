package org.apache.logging.log4j.core.time;

public interface PreciseClock extends Clock {
   void init(final MutableInstant mutableInstant);
}
