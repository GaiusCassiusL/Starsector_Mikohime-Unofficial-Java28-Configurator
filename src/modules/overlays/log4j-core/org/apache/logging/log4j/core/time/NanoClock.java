package org.apache.logging.log4j.core.time;

import org.apache.logging.log4j.plugins.di.Key;

public interface NanoClock {
   Key<NanoClock> KEY = new Key<NanoClock>() {};

   long nanoTime();
}
