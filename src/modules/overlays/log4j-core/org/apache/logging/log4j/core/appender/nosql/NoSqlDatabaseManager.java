package org.apache.logging.log4j.core.appender.nosql;

import java.util.Date;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

public final class NoSqlDatabaseManager<W> extends AbstractDatabaseManager {
   private static final NoSqlDatabaseManager.NoSQLDatabaseManagerFactory FACTORY = new NoSqlDatabaseManager.NoSQLDatabaseManagerFactory();
   private final NoSqlProvider<NoSqlConnection<W, ? extends NoSqlObject<W>>> provider;
   private NoSqlConnection<W, ? extends NoSqlObject<W>> connection;

   private NoSqlDatabaseManager(final String name, final int bufferSize, final NoSqlProvider<NoSqlConnection<W, ? extends NoSqlObject<W>>> provider) {
      super(name, bufferSize);
      this.provider = provider;
   }

   @Override
   protected void startupInternal() {
   }

   @Override
   protected boolean shutdownInternal() {
      return Closer.closeSilently(this.connection);
   }

   @Override
   protected void connectAndStart() {
      try {
         this.connection = this.provider.getConnection();
      } catch (Exception e) {
         throw new AppenderLoggingException("Failed to get connection from NoSQL connection provider.", e);
      }
   }

   @Override
   protected void writeInternal(final LogEvent event) {
      if (this.isRunning() && this.connection != null && !this.connection.isClosed()) {
         NoSqlObject<W> entity = (NoSqlObject<W>)this.connection.createObject();
         Message eventMessage = event.getMessage();
         if (eventMessage instanceof MapMessage) {
            this.setFields((MapMessage<?, ?>)eventMessage, entity);
         } else {
            this.setFields(event, entity);
         }

         this.connection.insertObject(entity);
      } else {
         throw new AppenderLoggingException("Cannot write logging event; NoSQL manager not connected to the database.");
      }
   }

   private void setFields(final MapMessage<?, ?> mapMessage, final NoSqlObject<W> noSqlObject) {
      mapMessage.forEach((key, value) -> noSqlObject.set(key, value));
   }

   private void setFields(final LogEvent event, final NoSqlObject<W> entity) {
      entity.set("level", event.getLevel());
      entity.set("loggerName", event.getLoggerName());
      entity.set("message", event.getMessage() == null ? null : event.getMessage().getFormattedMessage());
      StackTraceElement source = event.getSource();
      if (source == null) {
         entity.set("source", (Object)null);
      } else {
         entity.set("source", this.convertStackTraceElement(source));
      }

      Marker marker = event.getMarker();
      if (marker == null) {
         entity.set("marker", (Object)null);
      } else {
         entity.set("marker", this.buildMarkerEntity(marker));
      }

      entity.set("threadId", event.getThreadId());
      entity.set("threadName", event.getThreadName());
      entity.set("threadPriority", event.getThreadPriority());
      entity.set("millis", event.getTimeMillis());
      entity.set("date", new Date(event.getTimeMillis()));
      Throwable thrown = event.getThrown();
      if (thrown == null) {
         entity.set("thrown", (Object)null);
      } else {
         NoSqlObject<W> originalExceptionEntity = (NoSqlObject<W>)this.connection.createObject();
         NoSqlObject<W> exceptionEntity = originalExceptionEntity;
         exceptionEntity.set("type", thrown.getClass().getName());
         exceptionEntity.set("message", thrown.getMessage());
         exceptionEntity.set("stackTrace", this.convertStackTrace(thrown.getStackTrace()));

         while (thrown.getCause() != null) {
            thrown = thrown.getCause();
            NoSqlObject<W> causingExceptionEntity = (NoSqlObject<W>)this.connection.createObject();
            causingExceptionEntity.set("type", thrown.getClass().getName());
            causingExceptionEntity.set("message", thrown.getMessage());
            causingExceptionEntity.set("stackTrace", this.convertStackTrace(thrown.getStackTrace()));
            exceptionEntity.set("cause", causingExceptionEntity);
            exceptionEntity = causingExceptionEntity;
         }

         entity.set("thrown", originalExceptionEntity);
      }

      ReadOnlyStringMap contextMap = event.getContextData();
      if (contextMap == null) {
         entity.set("contextMap", (Object)null);
      } else {
         NoSqlObject<W> contextMapEntity = (NoSqlObject<W>)this.connection.createObject();
         contextMap.forEach((key, val) -> contextMapEntity.set(key, val));
         entity.set("contextMap", contextMapEntity);
      }

      ContextStack contextStack = event.getContextStack();
      if (contextStack == null) {
         entity.set("contextStack", (Object)null);
      } else {
         entity.set("contextStack", contextStack.asList().toArray());
      }
   }

   private NoSqlObject<W> buildMarkerEntity(final Marker marker) {
      NoSqlObject<W> entity = (NoSqlObject<W>)this.connection.createObject();
      entity.set("name", marker.getName());
      Marker[] parents = marker.getParents();
      if (parents != null) {
         NoSqlObject<W>[] parentEntities = new NoSqlObject[parents.length];

         for (int i = 0; i < parents.length; i++) {
            parentEntities[i] = this.buildMarkerEntity(parents[i]);
         }

         entity.set("parents", parentEntities);
      }

      return entity;
   }

   @Override
   protected boolean commitAndClose() {
      return true;
   }

   private NoSqlObject<W>[] convertStackTrace(final StackTraceElement[] stackTrace) {
      NoSqlObject<W>[] stackTraceEntities = (NoSqlObject<W>[])this.connection.createList(stackTrace.length);

      for (int i = 0; i < stackTrace.length; i++) {
         stackTraceEntities[i] = this.convertStackTraceElement(stackTrace[i]);
      }

      return stackTraceEntities;
   }

   private NoSqlObject<W> convertStackTraceElement(final StackTraceElement element) {
      NoSqlObject<W> elementEntity = (NoSqlObject<W>)this.connection.createObject();
      elementEntity.set("className", element.getClassName());
      elementEntity.set("methodName", element.getMethodName());
      elementEntity.set("fileName", element.getFileName());
      elementEntity.set("lineNumber", element.getLineNumber());
      return elementEntity;
   }

   public static NoSqlDatabaseManager<?> getNoSqlDatabaseManager(final String name, final int bufferSize, final NoSqlProvider<?> provider) {
      return AbstractDatabaseManager.getManager(name, new NoSqlDatabaseManager.FactoryData(bufferSize, provider), FACTORY);
   }

   private static final class FactoryData extends AbstractDatabaseManager.AbstractFactoryData {
      private final NoSqlProvider<?> provider;

      protected FactoryData(final int bufferSize, final NoSqlProvider<?> provider) {
         super(bufferSize, null);
         this.provider = provider;
      }
   }

   private static final class NoSQLDatabaseManagerFactory implements ManagerFactory<NoSqlDatabaseManager<?>, NoSqlDatabaseManager.FactoryData> {
      public NoSqlDatabaseManager<?> createManager(final String name, final NoSqlDatabaseManager.FactoryData data) {
         return new NoSqlDatabaseManager(name, data.getBufferSize(), (NoSqlProvider<NoSqlConnection<W, ? extends NoSqlObject<W>>>)data.provider);
      }
   }
}
