package org.apache.logging.log4j.core.layout;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;

public abstract class AbstractLayout implements Layout {
   protected static final Logger LOGGER = StatusLogger.getLogger();
   protected final Configuration configuration;
   protected long eventCount;
   protected final byte[] footer;
   protected final byte[] header;

   public AbstractLayout(final Configuration configuration, final byte[] header, final byte[] footer) {
      this.configuration = configuration;
      this.header = header;
      this.footer = footer;
   }

   public Configuration getConfiguration() {
      return this.configuration;
   }

   @Override
   public Map<String, String> getContentFormat() {
      return new HashMap<>();
   }

   @Override
   public byte[] getFooter() {
      return this.footer;
   }

   @Override
   public byte[] getHeader() {
      return this.header;
   }

   protected void markEvent() {
      this.eventCount++;
   }

   public void encode(final LogEvent event, final ByteBufferDestination destination) {
      byte[] data = this.toByteArray(event);
      destination.writeBytes(data, 0, data.length);
   }

   public abstract static class Builder<B extends AbstractLayout.Builder<B>> {
      @PluginConfiguration
      private Configuration configuration;
      @PluginBuilderAttribute
      private byte[] footer;
      @PluginBuilderAttribute
      private byte[] header;

      public B asBuilder() {
         return (B)Cast.cast(this);
      }

      public Configuration getConfiguration() {
         return this.configuration;
      }

      public byte[] getFooter() {
         return this.footer;
      }

      public byte[] getHeader() {
         return this.header;
      }

      public B setConfiguration(final Configuration configuration) {
         this.configuration = configuration;
         return this.asBuilder();
      }

      public B setFooter(final byte[] footer) {
         this.footer = footer;
         return this.asBuilder();
      }

      public B setHeader(final byte[] header) {
         this.header = header;
         return this.asBuilder();
      }
   }
}
