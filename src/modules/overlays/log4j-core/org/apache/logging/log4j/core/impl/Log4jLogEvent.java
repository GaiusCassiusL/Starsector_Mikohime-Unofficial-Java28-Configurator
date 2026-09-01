package org.apache.logging.log4j.core.impl;

import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.StringMap;

public class Log4jLogEvent implements LogEvent {
   private final String loggerFqcn;
   private final Marker marker;
   private final Level level;
   private final String loggerName;
   private Message message;
   private final MutableInstant instant = new MutableInstant();
   private final Throwable thrown;
   private ThrowableProxy thrownProxy;
   private final StringMap contextData;
   private final ContextStack contextStack;
   private long threadId;
   private String threadName;
   private int threadPriority;
   private StackTraceElement source;
   private boolean includeLocation;
   private boolean endOfBatch = false;
   private final long nanoTime;

   public static Log4jLogEvent.Builder newBuilder() {
      return new Log4jLogEvent.Builder().setLoggerName("");
   }

   public Log4jLogEvent() {
      this("", null, "", null, null, null, null, null, null, 0L, null, 0, null, 0L, 0, 0L);
   }

   private Log4jLogEvent(
      final String loggerName,
      final Marker marker,
      final String loggerFQCN,
      final Level level,
      final Message message,
      final Throwable thrown,
      final ThrowableProxy thrownProxy,
      final StringMap contextData,
      final ContextStack contextStack,
      final long threadId,
      final String threadName,
      final int threadPriority,
      final StackTraceElement source,
      final long timestampMillis,
      final int nanoOfMillisecond,
      final long nanoTime
   ) {
      this.loggerName = loggerName;
      this.marker = marker;
      this.loggerFqcn = loggerFQCN;
      this.level = level == null ? Level.OFF : level;
      this.message = message;
      this.thrown = thrown;
      this.thrownProxy = thrownProxy;
      this.contextData = contextData == null ? ContextDataFactory.createContextData() : contextData;
      this.contextStack = (ContextStack)(contextStack == null ? ThreadContext.EMPTY_STACK : contextStack);
      this.threadId = threadId;
      this.threadName = threadName;
      this.threadPriority = threadPriority;
      this.source = source;
      if (message instanceof LoggerNameAwareMessage) {
         ((LoggerNameAwareMessage)message).setLoggerName(loggerName);
      }

      this.nanoTime = nanoTime;
      long millis = message instanceof TimestampMessage ? ((TimestampMessage)message).getTimestamp() : timestampMillis;
      this.instant.initFromEpochMilli(millis, nanoOfMillisecond);
   }

   public Log4jLogEvent.Builder asBuilder() {
      return new Log4jLogEvent.Builder(this);
   }

   public Log4jLogEvent toImmutable() {
      if (this.getMessage() instanceof ReusableMessage) {
         this.makeMessageImmutable();
      }

      return this;
   }

   @Override
   public Level getLevel() {
      return this.level;
   }

   @Override
   public String getLoggerName() {
      return this.loggerName;
   }

   @Override
   public Message getMessage() {
      return this.message;
   }

   public void makeMessageImmutable() {
      this.message = new MementoMessage(this.message.getFormattedMessage(), this.message.getFormat(), this.message.getParameters());
   }

   @Override
   public long getThreadId() {
      if (this.threadId == 0L) {
         this.threadId = Thread.currentThread().getId();
      }

      return this.threadId;
   }

   @Override
   public String getThreadName() {
      if (this.threadName == null) {
         this.threadName = Thread.currentThread().getName();
      }

      return this.threadName;
   }

   @Override
   public int getThreadPriority() {
      if (this.threadPriority == 0) {
         this.threadPriority = Thread.currentThread().getPriority();
      }

      return this.threadPriority;
   }

   @Override
   public long getTimeMillis() {
      return this.instant.getEpochMillisecond();
   }

   @Override
   public Instant getInstant() {
      return this.instant;
   }

   @Override
   public Throwable getThrown() {
      return this.thrown;
   }

   @Override
   public ThrowableProxy getThrownProxy() {
      if (this.thrownProxy == null && this.thrown != null) {
         this.thrownProxy = new ThrowableProxy(this.thrown);
      }

      return this.thrownProxy;
   }

   @Override
   public Marker getMarker() {
      return this.marker;
   }

   @Override
   public String getLoggerFqcn() {
      return this.loggerFqcn;
   }

   @Override
   public ReadOnlyStringMap getContextData() {
      return this.contextData;
   }

   @Override
   public ContextStack getContextStack() {
      return this.contextStack;
   }

   @Override
   public StackTraceElement getSource() {
      if (this.source != null) {
         return this.source;
      } else if (this.loggerFqcn != null && this.includeLocation) {
         this.source = StackLocatorUtil.calcLocation(this.loggerFqcn);
         return this.source;
      } else {
         return null;
      }
   }

   @Override
   public boolean isIncludeLocation() {
      return this.includeLocation;
   }

   @Override
   public void setIncludeLocation(final boolean includeLocation) {
      this.includeLocation = includeLocation;
   }

   @Override
   public boolean isEndOfBatch() {
      return this.endOfBatch;
   }

   @Override
   public void setEndOfBatch(final boolean endOfBatch) {
      this.endOfBatch = endOfBatch;
   }

   @Override
   public long getNanoTime() {
      return this.nanoTime;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      String n = this.loggerName.isEmpty() ? "root" : this.loggerName;
      sb.append("Logger=").append(n);
      sb.append(" Level=").append(this.level.name());
      sb.append(" Message=").append(this.message == null ? null : this.message.getFormattedMessage());
      return sb.toString();
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         Log4jLogEvent that = (Log4jLogEvent)o;
         if (this.endOfBatch != that.endOfBatch) {
            return false;
         }

         if (this.includeLocation != that.includeLocation) {
            return false;
         }

         if (!this.instant.equals(that.instant)) {
            return false;
         }

         if (this.nanoTime != that.nanoTime) {
            return false;
         }

         if (this.loggerFqcn != null ? this.loggerFqcn.equals(that.loggerFqcn) : that.loggerFqcn == null) {
            if (this.level != null ? this.level.equals(that.level) : that.level == null) {
               if (this.source != null ? this.source.equals(that.source) : that.source == null) {
                  if (this.marker != null ? this.marker.equals(that.marker) : that.marker == null) {
                     if (this.contextData != null ? this.contextData.equals(that.contextData) : that.contextData == null) {
                        if (!this.message.equals(that.message)) {
                           return false;
                        }

                        if (!this.loggerName.equals(that.loggerName)) {
                           return false;
                        }

                        if (this.contextStack != null ? this.contextStack.equals(that.contextStack) : that.contextStack == null) {
                           if (this.threadId != that.threadId) {
                              return false;
                           }

                           if (this.threadName != null ? this.threadName.equals(that.threadName) : that.threadName == null) {
                              if (this.threadPriority != that.threadPriority) {
                                 return false;
                              } else {
                                 return !Objects.equals(this.thrown, that.thrown) ? false : Objects.equals(this.thrownProxy, that.thrownProxy);
                              }
                           } else {
                              return false;
                           }
                        } else {
                           return false;
                        }
                     } else {
                        return false;
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.loggerFqcn != null ? this.loggerFqcn.hashCode() : 0;
      result = 31 * result + (this.marker != null ? this.marker.hashCode() : 0);
      result = 31 * result + (this.level != null ? this.level.hashCode() : 0);
      result = 31 * result + this.loggerName.hashCode();
      result = 31 * result + this.message.hashCode();
      result = 31 * result + this.instant.hashCode();
      result = 31 * result + (int)(this.nanoTime ^ this.nanoTime >>> 32);
      result = 31 * result + (this.thrown != null ? this.thrown.hashCode() : 0);
      result = 31 * result + (this.thrownProxy != null ? this.thrownProxy.hashCode() : 0);
      result = 31 * result + (this.contextData != null ? this.contextData.hashCode() : 0);
      result = 31 * result + (this.contextStack != null ? this.contextStack.hashCode() : 0);
      result = 31 * result + (int)(this.threadId ^ this.threadId >>> 32);
      result = 31 * result + (this.threadName != null ? this.threadName.hashCode() : 0);
      result = 31 * result + this.threadPriority;
      result = 31 * result + (this.source != null ? this.source.hashCode() : 0);
      result = 31 * result + (this.includeLocation ? 1 : 0);
      return 31 * result + (this.endOfBatch ? 1 : 0);
   }

   public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<LogEvent> {
      private String loggerFqcn;
      private Marker marker;
      private Level level;
      private String loggerName;
      private Message message;
      private Throwable thrown;
      private final MutableInstant instant = new MutableInstant();
      private ThrowableProxy thrownProxy;
      private StringMap contextData;
      private ContextStack contextStack = ThreadContext.getImmutableStack();
      private long threadId;
      private String threadName;
      private int threadPriority;
      private StackTraceElement source;
      private boolean includeLocation;
      private boolean endOfBatch = false;
      private long nanoTime;
      private Clock clock;
      private ContextDataInjector contextDataInjector;

      public Builder() {
         this.initDefaultContextData();
      }

      public Builder(final LogEvent other) {
         Objects.requireNonNull(other);
         if (other instanceof RingBufferLogEvent) {
            ((RingBufferLogEvent)other).initializeBuilder(this);
         } else if (other instanceof MutableLogEvent) {
            ((MutableLogEvent)other).initializeBuilder(this);
         } else {
            this.loggerFqcn = other.getLoggerFqcn();
            this.marker = other.getMarker();
            this.level = other.getLevel();
            this.loggerName = other.getLoggerName();
            this.message = other.getMessage();
            this.instant.initFrom(other.getInstant());
            this.thrown = other.getThrown();
            this.contextStack = other.getContextStack();
            this.includeLocation = other.isIncludeLocation();
            this.endOfBatch = other.isEndOfBatch();
            this.nanoTime = other.getNanoTime();
            this.initDefaultContextData();
            if (other instanceof Log4jLogEvent) {
               Log4jLogEvent evt = (Log4jLogEvent)other;
               this.contextData = evt.contextData;
               this.thrownProxy = evt.thrownProxy;
               this.source = evt.source;
               this.threadId = evt.threadId;
               this.threadName = evt.threadName;
               this.threadPriority = evt.threadPriority;
            } else {
               if (other.getContextData() instanceof StringMap) {
                  this.contextData = (StringMap)other.getContextData();
               } else {
                  if (this.contextData.isFrozen()) {
                     this.contextData = ContextDataFactory.createContextData();
                  } else {
                     this.contextData.clear();
                  }

                  this.contextData.putAll(other.getContextData());
               }

               this.thrownProxy = other.getThrownProxy();
               this.source = other.getSource();
               this.threadId = other.getThreadId();
               this.threadName = other.getThreadName();
               this.threadPriority = other.getThreadPriority();
            }
         }
      }

      public Log4jLogEvent.Builder setLevel(final Level level) {
         this.level = level;
         return this;
      }

      public Log4jLogEvent.Builder setLoggerFqcn(final String loggerFqcn) {
         this.loggerFqcn = loggerFqcn;
         return this;
      }

      public Log4jLogEvent.Builder setLoggerName(final String loggerName) {
         this.loggerName = loggerName;
         return this;
      }

      public Log4jLogEvent.Builder setMarker(final Marker marker) {
         this.marker = marker;
         return this;
      }

      public Log4jLogEvent.Builder setMessage(final Message message) {
         this.message = message;
         return this;
      }

      public Log4jLogEvent.Builder setThrown(final Throwable thrown) {
         this.thrown = thrown;
         return this;
      }

      public Log4jLogEvent.Builder setTimeMillis(final long timeMillis) {
         this.instant.initFromEpochMilli(timeMillis, 0);
         return this;
      }

      public Log4jLogEvent.Builder setInstant(final Instant instant) {
         this.instant.initFrom(instant);
         return this;
      }

      public Log4jLogEvent.Builder setThrownProxy(final ThrowableProxy thrownProxy) {
         this.thrownProxy = thrownProxy;
         return this;
      }

      public Log4jLogEvent.Builder setContextData(final StringMap contextData) {
         this.contextData = contextData;
         return this;
      }

      public Log4jLogEvent.Builder setContextStack(final ContextStack contextStack) {
         this.contextStack = contextStack;
         return this;
      }

      public Log4jLogEvent.Builder setThreadId(final long threadId) {
         this.threadId = threadId;
         return this;
      }

      public Log4jLogEvent.Builder setThreadName(final String threadName) {
         this.threadName = threadName;
         return this;
      }

      public Log4jLogEvent.Builder setThreadPriority(final int threadPriority) {
         this.threadPriority = threadPriority;
         return this;
      }

      public Log4jLogEvent.Builder setSource(final StackTraceElement source) {
         this.source = source;
         return this;
      }

      public Log4jLogEvent.Builder setIncludeLocation(final boolean includeLocation) {
         this.includeLocation = includeLocation;
         return this;
      }

      public Log4jLogEvent.Builder setEndOfBatch(final boolean endOfBatch) {
         this.endOfBatch = endOfBatch;
         return this;
      }

      public Log4jLogEvent.Builder setNanoTime(final long nanoTime) {
         this.nanoTime = nanoTime;
         return this;
      }

      public Log4jLogEvent.Builder setClock(final Clock clock) {
         this.clock = clock;
         return this;
      }

      public Log4jLogEvent.Builder setContextDataInjector(final ContextDataInjector contextDataInjector) {
         this.contextDataInjector = contextDataInjector;
         return this;
      }

      public Log4jLogEvent build() {
         this.initTimeFields();
         Log4jLogEvent result = new Log4jLogEvent(
            this.loggerName,
            this.marker,
            this.loggerFqcn,
            this.level,
            this.message,
            this.thrown,
            this.thrownProxy,
            this.contextData,
            this.contextStack,
            this.threadId,
            this.threadName,
            this.threadPriority,
            this.source,
            this.instant.getEpochMillisecond(),
            this.instant.getNanoOfMillisecond(),
            this.nanoTime
         );
         result.setIncludeLocation(this.includeLocation);
         result.setEndOfBatch(this.endOfBatch);
         return result;
      }

      private void initTimeFields() {
         if (this.instant.getEpochMillisecond() == 0L) {
            if (this.message instanceof TimestampMessage) {
               this.instant.initFromEpochMilli(((TimestampMessage)this.message).getTimestamp(), 0);
            } else {
               this.instant.initFrom(this.clock != null ? this.clock : ClockFactory.getClock());
            }
         }
      }

      private void initDefaultContextData() {
         this.contextDataInjector = ContextDataInjectorFactory.createInjector();
         this.contextData = this.contextDataInjector.injectContextData(null, ContextDataFactory.createContextData());
      }
   }
}
