package org.apache.logging.log4j.core.selector;

import java.net.URI;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.plugins.di.Key;

public interface ContextSelector {
   long DEFAULT_STOP_TIMEOUT = 50L;
   Key<ContextSelector> KEY = new Key<ContextSelector>() {};

   default void shutdown(final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
      if (this.hasContext(fqcn, loader, currentContext)) {
         this.getContext(fqcn, loader, currentContext).stop(50L, TimeUnit.MILLISECONDS);
      }
   }

   default boolean hasContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
      return false;
   }

   LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext);

   default LoggerContext getContext(final String fqcn, final ClassLoader loader, final Entry<String, Object> entry, final boolean currentContext) {
      LoggerContext lc = this.getContext(fqcn, loader, currentContext);
      if (entry != null) {
         lc.putObject(entry.getKey(), entry.getValue());
      }

      return lc;
   }

   LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation);

   default LoggerContext getContext(
      final String fqcn, final ClassLoader loader, final Entry<String, Object> entry, final boolean currentContext, final URI configLocation
   ) {
      LoggerContext lc = this.getContext(fqcn, loader, currentContext, configLocation);
      if (entry != null) {
         lc.putObject(entry.getKey(), entry.getValue());
      }

      return lc;
   }

   List<LoggerContext> getLoggerContexts();

   void removeContext(LoggerContext context);

   default boolean isClassLoaderDependent() {
      return true;
   }
}
