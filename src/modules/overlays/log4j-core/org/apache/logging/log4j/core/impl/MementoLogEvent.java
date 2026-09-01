package org.apache.logging.log4j.core.impl;

import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.util.InternalApi;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;

@InternalApi
public class MementoLogEvent implements LogEvent {
   private final String loggerFqcn;
   private final String loggerName;
   private final MutableInstant instant = new MutableInstant();
   private final long nanoTime;
   private final Level level;
   private final Marker marker;
   private boolean locationRequired;
   private boolean endOfBatch;
   private final Message message;
   private final ReadOnlyStringMap contextData;
   private final ContextStack contextStack;
   private final StackTraceElement source;
   private final String threadName;
   private final long threadId;
   private final int threadPriority;
   private final Throwable thrown;
   private final ThrowableProxy thrownProxy;

   public MementoLogEvent(final LogEvent event) {
      this.loggerFqcn = event.getLoggerFqcn();
      this.loggerName = event.getLoggerName();
      this.instant.initFrom(event.getInstant());
      this.nanoTime = event.getNanoTime();
      this.level = event.getLevel();
      this.marker = event.getMarker();
      boolean includeLocation = event.isIncludeLocation();
      this.locationRequired = includeLocation;
      this.endOfBatch = event.isEndOfBatch();
      this.message = mementoOfMessage(event);
      if (this.instant.getEpochMillisecond() == 0L && this.message instanceof TimestampMessage) {
         this.instant.initFromEpochMilli(((TimestampMessage)this.message).getTimestamp(), 0);
      }

      this.contextData = memento(event.getContextData());
      this.contextStack = event.getContextStack();
      this.source = includeLocation ? event.getSource() : null;
      this.threadName = event.getThreadName();
      this.threadId = event.getThreadId();
      this.threadPriority = event.getThreadPriority();
      this.thrown = event.getThrown();
      this.thrownProxy = event.getThrownProxy();
   }

   public MementoLogEvent(final LogEvent event, final boolean includeLocation) {
      this.loggerFqcn = event.getLoggerFqcn();
      this.loggerName = event.getLoggerName();
      this.instant.initFrom(event.getInstant());
      this.nanoTime = event.getNanoTime();
      this.level = event.getLevel();
      this.marker = event.getMarker();
      this.locationRequired = includeLocation;
      this.endOfBatch = event.isEndOfBatch();
      this.message = mementoOfMessage(event);
      if (this.instant.getEpochMillisecond() == 0L && this.message instanceof TimestampMessage) {
         this.instant.initFromEpochMilli(((TimestampMessage)this.message).getTimestamp(), 0);
      }

      this.contextData = memento(event.getContextData());
      this.contextStack = event.getContextStack();
      this.source = includeLocation ? event.getSource() : null;
      this.threadName = event.getThreadName();
      this.threadId = event.getThreadId();
      this.threadPriority = event.getThreadPriority();
      this.thrown = event.getThrown();
      this.thrownProxy = event.getThrownProxy();
   }

   @Override
   public LogEvent toImmutable() {
      return this;
   }

   @Override
   public LogEvent toMemento(final boolean includeLocation) {
      return !this.locationRequired && includeLocation ? LogEvent.super.toMemento(true) : this;
   }

   @Override
   public LogEvent toMemento() {
      return this;
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
   public String getLoggerFqcn() {
      return this.loggerFqcn;
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
   public Marker getMarker() {
      return this.marker;
   }

   @Override
   public Message getMessage() {
      return this.message;
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
   public StackTraceElement getSource() {
      return this.source;
   }

   @Override
   public String getThreadName() {
      return this.threadName;
   }

   @Override
   public long getThreadId() {
      return this.threadId;
   }

   @Override
   public int getThreadPriority() {
      return this.threadPriority;
   }

   @Override
   public Throwable getThrown() {
      return this.thrown;
   }

   @Override
   public ThrowableProxy getThrownProxy() {
      return this.thrownProxy;
   }

   @Override
   public boolean isEndOfBatch() {
      return this.endOfBatch;
   }

   @Override
   public boolean isIncludeLocation() {
      return this.locationRequired;
   }

   @Override
   public void setEndOfBatch(boolean endOfBatch) {
      this.endOfBatch = endOfBatch;
   }

   @Override
   public void setIncludeLocation(boolean locationRequired) {
      this.locationRequired = locationRequired;
   }

   @Override
   public long getNanoTime() {
      return this.nanoTime;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         MementoLogEvent that = (MementoLogEvent)o;
         return this.nanoTime == that.nanoTime
            && this.locationRequired == that.locationRequired
            && this.endOfBatch == that.endOfBatch
            && this.threadId == that.threadId
            && this.threadPriority == that.threadPriority
            && Objects.equals(this.loggerFqcn, that.loggerFqcn)
            && Objects.equals(this.loggerName, that.loggerName)
            && Objects.equals(this.instant, that.instant)
            && Objects.equals(this.level, that.level)
            && Objects.equals(this.marker, that.marker)
            && Objects.equals(this.message, that.message)
            && Objects.equals(this.contextData, that.contextData)
            && Objects.equals(this.contextStack, that.contextStack)
            && Objects.equals(this.source, that.source)
            && Objects.equals(this.threadName, that.threadName)
            && Objects.equals(this.thrown, that.thrown)
            && Objects.equals(this.thrownProxy, that.thrownProxy);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(
         this.loggerFqcn,
         this.loggerName,
         this.instant,
         this.nanoTime,
         this.level,
         this.marker,
         this.locationRequired,
         this.endOfBatch,
         this.message,
         this.contextData,
         this.contextStack,
         this.source,
         this.threadName,
         this.threadId,
         this.threadPriority,
         this.thrown,
         this.thrownProxy
      );
   }

   @Override
   public String toString() {
      String n = this.loggerName.isEmpty() ? "root" : this.loggerName;
      return "Logger=" + n + " Level=" + this.level.name() + " Message=" + (this.message == null ? "" : this.message.getFormattedMessage());
   }

   private static ReadOnlyStringMap memento(final ReadOnlyStringMap readOnlyMap) {
      if (readOnlyMap instanceof StringMap && !((StringMap)readOnlyMap).isFrozen()) {
         StringMap data = ContextDataFactory.createContextData(readOnlyMap);
         data.freeze();
         return data;
      } else {
         return readOnlyMap;
      }
   }

   private static Message mementoOfMessage(final LogEvent event) {
      Message message = event.getMessage();
      if (message instanceof LoggerNameAwareMessage) {
         ((LoggerNameAwareMessage)message).setLoggerName(event.getLoggerName());
      }

      return message instanceof ReusableMessage ? ((ReusableMessage)message).memento() : message;
   }
}
