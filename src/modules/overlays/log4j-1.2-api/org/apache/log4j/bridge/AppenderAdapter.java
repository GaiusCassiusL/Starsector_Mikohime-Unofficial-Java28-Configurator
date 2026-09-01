package org.apache.log4j.bridge;

import org.apache.log4j.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.util.Strings;

public final class AppenderAdapter {
   private final Appender appender;
   private final AppenderAdapter.Adapter adapter;

   public static org.apache.logging.log4j.core.Appender adapt(final Appender appender) {
      if (appender instanceof org.apache.logging.log4j.core.Appender) {
         return (org.apache.logging.log4j.core.Appender)appender;
      } else if (appender instanceof AppenderWrapper) {
         return ((AppenderWrapper)appender).getAppender();
      } else {
         return appender != null ? new AppenderAdapter(appender).getAdapter() : null;
      }
   }

   private AppenderAdapter(final Appender appender) {
      this.appender = appender;
      Filter appenderFilter = FilterAdapter.adapt(appender.getFilter());
      String name = appender.getName();
      if (Strings.isEmpty(name)) {
         name = String.format("0x%08x", appender.hashCode());
      }

      this.adapter = new AppenderAdapter.Adapter(name, appenderFilter, null, true, null);
   }

   public AppenderAdapter.Adapter getAdapter() {
      return this.adapter;
   }

   public class Adapter extends AbstractAppender {
      protected Adapter(final String name, final Filter filter, final Layout layout, final boolean ignoreExceptions, final Property[] properties) {
         super(name, filter, layout, ignoreExceptions, properties);
      }

      public void append(final LogEvent event) {
         AppenderAdapter.this.appender.doAppend(new LogEventAdapter(event));
      }

      public void stop() {
         AppenderAdapter.this.appender.close();
      }

      public Appender getAppender() {
         return AppenderAdapter.this.appender;
      }
   }
}
