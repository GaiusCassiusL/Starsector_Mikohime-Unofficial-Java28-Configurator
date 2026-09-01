package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.plugins.di.Key;

public interface ShutdownCallbackRegistry {
   Key<ShutdownCallbackRegistry> KEY = Key.forClass(ShutdownCallbackRegistry.class);
   Marker SHUTDOWN_HOOK_MARKER = MarkerManager.getMarker("SHUTDOWN HOOK");

   Cancellable addShutdownCallback(Runnable callback);
}
