package org.apache.logging.log4j.core.appender;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;

public abstract class AbstractOutputStreamAppender<M extends OutputStreamManager> extends AbstractAppender {
   private final boolean immediateFlush;
   private final M manager;

   protected AbstractOutputStreamAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final boolean ignoreExceptions,
      final boolean immediateFlush,
      final Property[] properties,
      final M manager
   ) {
      super(name, filter, layout, ignoreExceptions, properties);
      this.manager = manager;
      this.immediateFlush = immediateFlush;
   }

   public boolean getImmediateFlush() {
      return this.immediateFlush;
   }

   public M getManager() {
      return this.manager;
   }

   @Override
   public void start() {
      if (this.getLayout() == null) {
         LOGGER.error("No layout set for the appender named [" + this.getName() + "].");
      }

      if (this.manager == null) {
         LOGGER.error("No OutputStreamManager set for the appender named [" + this.getName() + "].");
      }

      super.start();
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      return this.stop(timeout, timeUnit, true);
   }

   @Override
   protected boolean stop(final long timeout, final TimeUnit timeUnit, final boolean changeLifeCycleState) {
      boolean stopped = super.stop(timeout, timeUnit, changeLifeCycleState);
      stopped &= this.manager.stop(timeout, timeUnit);
      if (changeLifeCycleState) {
         this.setStopped();
      }

      LOGGER.debug("Appender {} stopped with status {}", this.getName(), stopped);
      return stopped;
   }

   @Override
   public void append(final LogEvent event) {
      try {
         this.tryAppend(event);
      } catch (AppenderLoggingException ex) {
         this.error("Unable to write to stream " + this.manager.getName() + " for appender " + this.getName(), event, ex);
         throw ex;
      }
   }

   private void tryAppend(final LogEvent event) {
      if (Constants.ENABLE_DIRECT_ENCODERS) {
         this.directEncodeEvent(event);
      } else {
         this.writeByteArrayToManager(event);
      }
   }

   protected void directEncodeEvent(final LogEvent event) {
      this.getLayout().encode(event, this.manager);
      if (this.immediateFlush || event.isEndOfBatch()) {
         this.manager.flush();
      }
   }

   protected void writeByteArrayToManager(final LogEvent event) {
      byte[] bytes = this.getLayout().toByteArray(event);
      if (bytes != null && bytes.length > 0) {
         this.manager.write(bytes, this.immediateFlush || event.isEndOfBatch());
      }
   }

   public abstract static class Builder<B extends AbstractOutputStreamAppender.Builder<B>> extends AbstractAppender.Builder<B> {
      @PluginBuilderAttribute
      private boolean bufferedIo = true;
      @PluginBuilderAttribute
      private int bufferSize = Constants.ENCODER_BYTE_BUFFER_SIZE;
      @PluginBuilderAttribute
      private boolean immediateFlush = true;

      public int getBufferSize() {
         return this.bufferSize;
      }

      public boolean isBufferedIo() {
         return this.bufferedIo;
      }

      public boolean isImmediateFlush() {
         return this.immediateFlush;
      }

      public B setImmediateFlush(final boolean immediateFlush) {
         this.immediateFlush = immediateFlush;
         return this.asBuilder();
      }

      public B setBufferedIo(final boolean bufferedIo) {
         this.bufferedIo = bufferedIo;
         return this.asBuilder();
      }

      public B setBufferSize(final int bufferSize) {
         this.bufferSize = bufferSize;
         return this.asBuilder();
      }
   }
}
