package org.apache.logging.log4j.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Supplier;

public class Logger extends AbstractLogger implements Supplier<LoggerConfig> {
   protected volatile Logger.PrivateConfig privateConfig;
   private final LoggerContext context;

   protected Logger(final LoggerContext context, final String name, final MessageFactory messageFactory) {
      super(name, messageFactory);
      this.context = context;
      this.privateConfig = new Logger.PrivateConfig(context.getConfiguration(), this);
   }

   public Logger getParent() {
      LoggerConfig lc = this.privateConfig.loggerConfig.getName().equals(this.getName())
         ? this.privateConfig.loggerConfig.getParent()
         : this.privateConfig.loggerConfig;
      if (lc == null) {
         return null;
      }

      String lcName = lc.getName();
      MessageFactory messageFactory = this.getMessageFactory();
      return this.context.hasLogger(lcName, messageFactory) ? this.context.getLogger(lcName, messageFactory) : new Logger(this.context, lcName, messageFactory);
   }

   public LoggerContext getContext() {
      return this.context;
   }

   public synchronized void setLevel(final Level level) {
      if (level != this.getLevel()) {
         Level actualLevel;
         if (level != null) {
            actualLevel = level;
         } else {
            Logger parent = this.getParent();
            actualLevel = parent != null ? parent.getLevel() : this.privateConfig.loggerConfigLevel;
         }

         this.privateConfig = new Logger.PrivateConfig(this.privateConfig, actualLevel);
      }
   }

   public LoggerConfig get() {
      return this.privateConfig.loggerConfig;
   }

   protected boolean requiresLocation() {
      return this.privateConfig.requiresLocation;
   }

   public void logMessage(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable t) {
      Message msg = (Message)(message == null ? new SimpleMessage("") : message);
      ReliabilityStrategy strategy = this.privateConfig.loggerConfig.getReliabilityStrategy();
      strategy.log(this, this.getName(), fqcn, marker, level, msg, t);
   }

   protected void log(
      final Level level, final Marker marker, final String fqcn, final StackTraceElement location, final Message message, final Throwable throwable
   ) {
      ReliabilityStrategy strategy = this.privateConfig.loggerConfig.getReliabilityStrategy();
      strategy.log(this, this.getName(), fqcn, location, marker, level, message, throwable);
   }

   public boolean isEnabled(final Level level, final Marker marker, final String message, final Throwable t) {
      return this.privateConfig.filter(level, marker, message, t);
   }

   public boolean isEnabled(final Level level, final Marker marker, final String message) {
      return this.privateConfig.filter(level, marker, message);
   }

   public boolean isEnabled(final Level level, final Marker marker, final String message, final Object... params) {
      return this.privateConfig.filter(level, marker, message, params);
   }

   public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
      return this.privateConfig.filter(level, marker, message, p0);
   }

   public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0, final Object p1) {
      return this.privateConfig.filter(level, marker, message, p0, p1);
   }

   public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
      return this.privateConfig.filter(level, marker, message, p0, p1, p2);
   }

   public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
      return this.privateConfig.filter(level, marker, message, p0, p1, p2, p3);
   }

   public boolean isEnabled(
      final Level level, final Marker marker, final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4
   ) {
      return this.privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4);
   }

   public boolean isEnabled(
      final Level level,
      final Marker marker,
      final String message,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5
   ) {
      return this.privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5);
   }

   public boolean isEnabled(
      final Level level,
      final Marker marker,
      final String message,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6
   ) {
      return this.privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5, p6);
   }

   public boolean isEnabled(
      final Level level,
      final Marker marker,
      final String message,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7
   ) {
      return this.privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
   }

   public boolean isEnabled(
      final Level level,
      final Marker marker,
      final String message,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7,
      final Object p8
   ) {
      return this.privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
   }

   public boolean isEnabled(
      final Level level,
      final Marker marker,
      final String message,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7,
      final Object p8,
      final Object p9
   ) {
      return this.privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
   }

   public boolean isEnabled(final Level level, final Marker marker, final CharSequence message, final Throwable t) {
      return this.privateConfig.filter(level, marker, message, t);
   }

   public boolean isEnabled(final Level level, final Marker marker, final Object message, final Throwable t) {
      return this.privateConfig.filter(level, marker, message, t);
   }

   public boolean isEnabled(final Level level, final Marker marker, final Message message, final Throwable t) {
      return this.privateConfig.filter(level, marker, message, t);
   }

   public void addAppender(final Appender appender) {
      this.privateConfig.config.addLoggerAppender(this, appender);
   }

   public void removeAppender(final Appender appender) {
      this.privateConfig.loggerConfig.removeAppender(appender.getName());
   }

   public Map<String, Appender> getAppenders() {
      return this.privateConfig.loggerConfig.getAppenders();
   }

   public Iterator<Filter> getFilters() {
      Filter filter = this.privateConfig.loggerConfig.getFilter();
      if (filter == null) {
         return Collections.emptyIterator();
      }

      if (filter instanceof CompositeFilter) {
         return ((CompositeFilter)filter).iterator();
      }

      List<Filter> filters = new ArrayList<>();
      filters.add(filter);
      return filters.iterator();
   }

   public Level getLevel() {
      return this.privateConfig.loggerConfigLevel;
   }

   public int filterCount() {
      Filter filter = this.privateConfig.loggerConfig.getFilter();
      if (filter == null) {
         return 0;
      } else {
         return filter instanceof CompositeFilter ? ((CompositeFilter)filter).size() : 1;
      }
   }

   public void addFilter(final Filter filter) {
      this.privateConfig.config.addLoggerFilter(this, filter);
   }

   public boolean isAdditive() {
      return this.privateConfig.loggerConfig.isAdditive();
   }

   public void setAdditive(final boolean additive) {
      this.privateConfig.config.setLoggerAdditive(this, additive);
   }

   public LogBuilder atLevel(final Level level) {
      return this.privateConfig.hasFilter() ? this.getLogBuilder(level) : super.atLevel(level);
   }

   protected void updateConfiguration(final Configuration newConfig) {
      this.privateConfig = new Logger.PrivateConfig(newConfig, this);
   }

   public String toString() {
      String nameLevel = this.getName() + ":" + this.getLevel();
      if (this.context == null) {
         return nameLevel;
      }

      String contextName = this.context.getName();
      return contextName == null ? nameLevel : nameLevel + " in " + contextName;
   }

   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Logger that = (Logger)o;
         return this.getName().equals(that.getName());
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.getName().hashCode();
   }

   protected class PrivateConfig {
      public final LoggerConfig loggerConfig;
      public final Configuration config;
      private final Level loggerConfigLevel;
      private final int intLevel;
      private final Logger logger;
      private final boolean requiresLocation;

      public PrivateConfig(final Configuration config, final Logger logger) {
         this.config = config;
         this.loggerConfig = config.getLoggerConfig(Logger.this.getName());
         this.loggerConfigLevel = this.loggerConfig.getLevel();
         this.intLevel = this.loggerConfigLevel.intLevel();
         this.logger = logger;
         this.requiresLocation = this.loggerConfig.requiresLocation();
      }

      public PrivateConfig(final Logger.PrivateConfig pc, final Level level) {
         this.config = pc.config;
         this.loggerConfig = pc.loggerConfig;
         this.loggerConfigLevel = level;
         this.intLevel = this.loggerConfigLevel.intLevel();
         this.logger = pc.logger;
         this.requiresLocation = this.loggerConfig.requiresLocation();
      }

      public PrivateConfig(final Logger.PrivateConfig pc, final LoggerConfig lc) {
         this.config = pc.config;
         this.loggerConfig = lc;
         this.loggerConfigLevel = lc.getLevel();
         this.intLevel = this.loggerConfigLevel.intLevel();
         this.logger = pc.logger;
         this.requiresLocation = this.loggerConfig.requiresLocation();
      }

      public void logEvent(final LogEvent event) {
         this.loggerConfig.log(event);
      }

      boolean hasFilter() {
         return this.config.getFilter() != null;
      }

      boolean filter(final Level level, final Marker marker, final String msg) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(final Level level, final Marker marker, final String msg, final Throwable t) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, t);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(final Level level, final Marker marker, final String msg, final Object... p1) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p1);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(final Level level, final Marker marker, final String msg, final Object p0) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(final Level level, final Marker marker, final String msg, final Object p0, final Object p1) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0, p1);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0, p1, p2);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2, final Object p3) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0, p1, p2, p3);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(
         final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4
      ) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0, p1, p2, p3, p4);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(
         final Level level,
         final Marker marker,
         final String msg,
         final Object p0,
         final Object p1,
         final Object p2,
         final Object p3,
         final Object p4,
         final Object p5
      ) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0, p1, p2, p3, p4, p5);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(
         final Level level,
         final Marker marker,
         final String msg,
         final Object p0,
         final Object p1,
         final Object p2,
         final Object p3,
         final Object p4,
         final Object p5,
         final Object p6
      ) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(
         final Level level,
         final Marker marker,
         final String msg,
         final Object p0,
         final Object p1,
         final Object p2,
         final Object p3,
         final Object p4,
         final Object p5,
         final Object p6,
         final Object p7
      ) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(
         final Level level,
         final Marker marker,
         final String msg,
         final Object p0,
         final Object p1,
         final Object p2,
         final Object p3,
         final Object p4,
         final Object p5,
         final Object p6,
         final Object p7,
         final Object p8
      ) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(
         final Level level,
         final Marker marker,
         final String msg,
         final Object p0,
         final Object p1,
         final Object p2,
         final Object p3,
         final Object p4,
         final Object p5,
         final Object p6,
         final Object p7,
         final Object p8,
         final Object p9
      ) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(final Level level, final Marker marker, final CharSequence msg, final Throwable t) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, t);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(final Level level, final Marker marker, final Object msg, final Throwable t) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, t);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      boolean filter(final Level level, final Marker marker, final Message msg, final Throwable t) {
         Filter filter = this.config.getFilter();
         if (filter != null) {
            Filter.Result r = filter.filter(this.logger, level, marker, msg, t);
            if (r != Filter.Result.NEUTRAL) {
               return r == Filter.Result.ACCEPT;
            }
         }

         return level != null && this.intLevel >= level.intLevel();
      }

      @Override
      public String toString() {
         StringBuilder builder = new StringBuilder();
         builder.append("PrivateConfig [loggerConfig=");
         builder.append(this.loggerConfig);
         builder.append(", config=");
         builder.append(this.config);
         builder.append(", loggerConfigLevel=");
         builder.append(this.loggerConfigLevel);
         builder.append(", intLevel=");
         builder.append(this.intLevel);
         builder.append(", logger=");
         builder.append(this.logger);
         builder.append("]");
         return builder.toString();
      }
   }
}
