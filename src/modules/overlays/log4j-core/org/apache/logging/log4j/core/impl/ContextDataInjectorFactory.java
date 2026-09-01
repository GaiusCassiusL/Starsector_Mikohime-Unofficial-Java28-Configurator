package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.spi.CopyOnWrite;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

public class ContextDataInjectorFactory {
   public static ContextDataInjector createInjector() {
      String className = PropertiesUtil.getProperties().getStringProperty(Log4jPropertyKey.THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME);
      if (className == null) {
         return createDefaultInjector();
      }

      try {
         Class<? extends ContextDataInjector> cls = Loader.loadClass(className).asSubclass(ContextDataInjector.class);
         return cls.newInstance();
      } catch (Exception dynamicFailed) {
         ContextDataInjector result = createDefaultInjector();
         StatusLogger.getLogger()
            .warn("Could not create ContextDataInjector for '{}', using default {}: {}", className, result.getClass().getName(), dynamicFailed);
         return result;
      }
   }

   private static ContextDataInjector createDefaultInjector() {
      ReadOnlyThreadContextMap threadContextMap = ThreadContext.getThreadContextMap();
      if (threadContextMap instanceof DefaultThreadContextMap || threadContextMap == null) {
         return new ThreadContextDataInjector.ForDefaultThreadContextMap();
      } else {
         return threadContextMap instanceof CopyOnWrite
            ? new ThreadContextDataInjector.ForCopyOnWriteThreadContextMap()
            : new ThreadContextDataInjector.ForGarbageFreeThreadContextMap();
      }
   }
}
