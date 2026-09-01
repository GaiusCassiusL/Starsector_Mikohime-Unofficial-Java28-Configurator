package org.apache.logging.log4j.core.time;

import org.apache.logging.log4j.plugins.di.Key;

public interface Clock {
   Key<Clock> KEY = new Key<Clock>() {};

   long currentTimeMillis();
}
