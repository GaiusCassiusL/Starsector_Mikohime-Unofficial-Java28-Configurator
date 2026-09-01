package org.apache.logging.log4j.core.layout;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.StringEncoder;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyKey;
import org.apache.logging.log4j.util.StringBuilders;

public abstract class AbstractStringLayout extends AbstractLayout implements StringLayout {
   protected static final int DEFAULT_STRING_BUILDER_SIZE = 1024;
   protected static final int MAX_STRING_BUILDER_SIZE = Math.max(1024, size(Log4jPropertyKey.GC_LAYOUT_STRING_BUILDER_MAX_SIZE, 2048));
   private static final ThreadLocal<StringBuilder> threadLocal = new ThreadLocal<>();
   private Encoder<StringBuilder> textEncoder;
   private final Charset charset;
   private final AbstractStringLayout.Serializer footerSerializer;
   private final AbstractStringLayout.Serializer headerSerializer;

   protected static StringBuilder getStringBuilder() {
      if (AbstractLogger.getRecursionDepth() > 1) {
         return new StringBuilder(1024);
      }

      StringBuilder result = threadLocal.get();
      if (result == null) {
         result = new StringBuilder(1024);
         threadLocal.set(result);
      }

      trimToMaxSize(result);
      result.setLength(0);
      return result;
   }

   private static int size(final PropertyKey property, final int defaultValue) {
      return PropertiesUtil.getProperties().getIntegerProperty(property, defaultValue);
   }

   protected static void trimToMaxSize(final StringBuilder stringBuilder) {
      StringBuilders.trimToMaxSize(stringBuilder, MAX_STRING_BUILDER_SIZE);
   }

   protected AbstractStringLayout(final Charset charset) {
      this(charset, (byte[])null, (byte[])null);
   }

   protected AbstractStringLayout(final Charset aCharset, final byte[] header, final byte[] footer) {
      super(null, header, footer);
      this.headerSerializer = null;
      this.footerSerializer = null;
      this.charset = aCharset == null ? StandardCharsets.UTF_8 : aCharset;
      this.textEncoder = Constants.ENABLE_DIRECT_ENCODERS ? new StringBuilderEncoder(this.charset) : null;
   }

   protected AbstractStringLayout(
      final Configuration config,
      final Charset aCharset,
      final AbstractStringLayout.Serializer headerSerializer,
      final AbstractStringLayout.Serializer footerSerializer
   ) {
      super(config, null, null);
      this.headerSerializer = headerSerializer;
      this.footerSerializer = footerSerializer;
      this.charset = aCharset == null ? StandardCharsets.UTF_8 : aCharset;
      this.textEncoder = Constants.ENABLE_DIRECT_ENCODERS ? new StringBuilderEncoder(this.charset) : null;
   }

   protected byte[] getBytes(final String s) {
      return s.getBytes(this.charset);
   }

   @Override
   public Charset getCharset() {
      return this.charset;
   }

   @Override
   public String getContentType() {
      return "text/plain";
   }

   @Override
   public byte[] getFooter() {
      return this.serializeToBytes(this.footerSerializer, super.getFooter());
   }

   public AbstractStringLayout.Serializer getFooterSerializer() {
      return this.footerSerializer;
   }

   @Override
   public byte[] getHeader() {
      return this.serializeToBytes(this.headerSerializer, super.getHeader());
   }

   public AbstractStringLayout.Serializer getHeaderSerializer() {
      return this.headerSerializer;
   }

   private DefaultLogEventFactory getLogEventFactory() {
      return DefaultLogEventFactory.newInstance();
   }

   protected Encoder<StringBuilder> getStringBuilderEncoder() {
      if (this.textEncoder == null) {
         this.textEncoder = new StringBuilderEncoder(this.getCharset());
      }

      return this.textEncoder;
   }

   protected byte[] serializeToBytes(final AbstractStringLayout.Serializer serializer, final byte[] defaultValue) {
      String serializable = this.serializeToString(serializer);
      return serializable == null ? defaultValue : StringEncoder.toBytes(serializable, this.getCharset());
   }

   protected String serializeToString(final AbstractStringLayout.Serializer serializer) {
      if (serializer == null) {
         return null;
      }

      LoggerConfig rootLogger = this.getConfiguration().getRootLogger();
      LogEvent logEvent = this.getLogEventFactory().createEvent(rootLogger.getName(), null, "", rootLogger.getLevel(), null, null, null);
      return serializer.toSerializable(logEvent);
   }

   @Override
   public byte[] toByteArray(final LogEvent event) {
      return this.getBytes(this.toSerializable(event));
   }

   @Override
   public abstract String toSerializable(LogEvent event);

   public abstract static class Builder<B extends AbstractStringLayout.Builder<B>> extends AbstractLayout.Builder<B> {
      @PluginBuilderAttribute("charset")
      private Charset charset;
      @PluginElement("footerSerializer")
      private AbstractStringLayout.Serializer footerSerializer;
      @PluginElement("headerSerializer")
      private AbstractStringLayout.Serializer headerSerializer;

      public Charset getCharset() {
         return this.charset;
      }

      public AbstractStringLayout.Serializer getFooterSerializer() {
         return this.footerSerializer;
      }

      public AbstractStringLayout.Serializer getHeaderSerializer() {
         return this.headerSerializer;
      }

      public B setCharset(final Charset charset) {
         this.charset = charset;
         return this.asBuilder();
      }

      public B setFooterSerializer(final AbstractStringLayout.Serializer footerSerializer) {
         this.footerSerializer = footerSerializer;
         return this.asBuilder();
      }

      public B setHeaderSerializer(final AbstractStringLayout.Serializer headerSerializer) {
         this.headerSerializer = headerSerializer;
         return this.asBuilder();
      }
   }

   public interface Serializer extends AbstractStringLayout.Serializer2 {
      String toSerializable(final LogEvent event);

      default boolean requiresLocation() {
         return false;
      }

      @Override
      default StringBuilder toSerializable(final LogEvent event, final StringBuilder builder) {
         builder.append(this.toSerializable(event));
         return builder;
      }
   }

   public interface Serializer2 {
      StringBuilder toSerializable(final LogEvent event, final StringBuilder builder);
   }
}
