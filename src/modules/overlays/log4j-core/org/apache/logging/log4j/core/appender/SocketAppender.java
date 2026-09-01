package org.apache.logging.log4j.core.appender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.DatagramSocketManager;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.core.net.SslSocketManager;
import org.apache.logging.log4j.core.net.TcpSocketManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.plugins.validation.constraints.ValidPort;

@Configurable(elementType = "appender", printObject = true)
@Plugin("Socket")
public class SocketAppender extends AbstractOutputStreamAppender<AbstractSocketManager> {
   private final Object advertisement;
   private final Advertiser advertiser;

   @PluginFactory
   public static SocketAppender.Builder newBuilder() {
      return new SocketAppender.Builder();
   }

   protected SocketAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final AbstractSocketManager manager,
      final boolean ignoreExceptions,
      final boolean immediateFlush,
      final Advertiser advertiser
   ) {
      super(name, layout, filter, ignoreExceptions, immediateFlush, null, manager);
      if (advertiser != null) {
         Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
         configuration.putAll(manager.getContentFormat());
         configuration.put("contentType", layout.getContentType());
         configuration.put("name", name);
         this.advertisement = advertiser.advertise(configuration);
      } else {
         this.advertisement = null;
      }

      this.advertiser = advertiser;
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      this.setStopping();
      super.stop(timeout, timeUnit, false);
      if (this.advertiser != null) {
         this.advertiser.unadvertise(this.advertisement);
      }

      this.setStopped();
      return true;
   }

   protected static AbstractSocketManager createSocketManager(
      final String name,
      Protocol protocol,
      final String host,
      final int port,
      final int connectTimeoutMillis,
      final SslConfiguration sslConfig,
      final int reconnectDelayMillis,
      final boolean immediateFail,
      final Layout layout,
      final int bufferSize,
      final SocketOptions socketOptions
   ) {
      if (protocol == Protocol.TCP && sslConfig != null) {
         protocol = Protocol.SSL;
      }

      if (protocol != Protocol.SSL && sslConfig != null) {
         LOGGER.info("Appender {} ignoring SSL configuration for {} protocol", name, protocol);
      }

      switch (protocol) {
         case TCP:
            return TcpSocketManager.getSocketManager(host, port, connectTimeoutMillis, reconnectDelayMillis, immediateFail, layout, bufferSize, socketOptions);
         case UDP:
            return DatagramSocketManager.getSocketManager(host, port, layout, bufferSize);
         case SSL:
            return SslSocketManager.getSocketManager(
               sslConfig, host, port, connectTimeoutMillis, reconnectDelayMillis, immediateFail, layout, bufferSize, socketOptions
            );
         default:
            throw new IllegalArgumentException(protocol.toString());
      }
   }

   @Override
   protected void directEncodeEvent(final LogEvent event) {
      this.writeByteArrayToManager(event);
   }

   public abstract static class AbstractBuilder<B extends SocketAppender.AbstractBuilder<B>> extends AbstractOutputStreamAppender.Builder<B> {
      @PluginBuilderAttribute
      private boolean advertise;
      @PluginBuilderAttribute
      private int connectTimeoutMillis;
      @PluginBuilderAttribute
      @ValidHost
      private String host = "localhost";
      @PluginBuilderAttribute
      private boolean immediateFail = true;
      @PluginBuilderAttribute
      @ValidPort
      private int port;
      @PluginBuilderAttribute
      private Protocol protocol = Protocol.TCP;
      @PluginBuilderAttribute
      @PluginAliases({"reconnectDelay", "reconnectionDelay", "delayMillis", "reconnectionDelayMillis"})
      private int reconnectDelayMillis;
      @PluginElement("SocketOptions")
      private SocketOptions socketOptions;
      @PluginElement("SslConfiguration")
      @PluginAliases("SslConfig")
      private SslConfiguration sslConfiguration;

      public boolean getAdvertise() {
         return this.advertise;
      }

      public int getConnectTimeoutMillis() {
         return this.connectTimeoutMillis;
      }

      public String getHost() {
         return this.host;
      }

      public int getPort() {
         return this.port;
      }

      public Protocol getProtocol() {
         return this.protocol;
      }

      public SslConfiguration getSslConfiguration() {
         return this.sslConfiguration;
      }

      public boolean getImmediateFail() {
         return this.immediateFail;
      }

      public B setAdvertise(final boolean advertise) {
         this.advertise = advertise;
         return this.asBuilder();
      }

      public B setConnectTimeoutMillis(final int connectTimeoutMillis) {
         this.connectTimeoutMillis = connectTimeoutMillis;
         return this.asBuilder();
      }

      public B setHost(final String host) {
         this.host = host;
         return this.asBuilder();
      }

      public B setImmediateFail(final boolean immediateFail) {
         this.immediateFail = immediateFail;
         return this.asBuilder();
      }

      public B setPort(final int port) {
         this.port = port;
         return this.asBuilder();
      }

      public B setProtocol(final Protocol protocol) {
         this.protocol = protocol;
         return this.asBuilder();
      }

      public B setReconnectDelayMillis(final int reconnectDelayMillis) {
         this.reconnectDelayMillis = reconnectDelayMillis;
         return this.asBuilder();
      }

      public B setSocketOptions(final SocketOptions socketOptions) {
         this.socketOptions = socketOptions;
         return this.asBuilder();
      }

      public B setSslConfiguration(final SslConfiguration sslConfiguration) {
         this.sslConfiguration = sslConfiguration;
         return this.asBuilder();
      }

      public int getReconnectDelayMillis() {
         return this.reconnectDelayMillis;
      }

      public SocketOptions getSocketOptions() {
         return this.socketOptions;
      }
   }

   public static class Builder
      extends SocketAppender.AbstractBuilder<SocketAppender.Builder>
      implements org.apache.logging.log4j.plugins.util.Builder<SocketAppender> {
      public SocketAppender build() {
         boolean immediateFlush = this.isImmediateFlush();
         boolean bufferedIo = this.isBufferedIo();
         Layout layout = this.getLayout();
         if (layout == null) {
            SocketAppender.LOGGER.error("No layout provided for SocketAppender");
            return null;
         }

         String name = this.getName();
         if (name == null) {
            SocketAppender.LOGGER.error("No name provided for SocketAppender");
            return null;
         }

         Protocol protocol = this.getProtocol();
         Protocol actualProtocol = protocol != null ? protocol : Protocol.TCP;
         if (actualProtocol == Protocol.UDP) {
            immediateFlush = true;
         }

         AbstractSocketManager manager = SocketAppender.createSocketManager(
            name,
            actualProtocol,
            this.getHost(),
            this.getPort(),
            this.getConnectTimeoutMillis(),
            this.getSslConfiguration(),
            this.getReconnectDelayMillis(),
            this.getImmediateFail(),
            layout,
            this.getBufferSize(),
            this.getSocketOptions()
         );
         return new SocketAppender(
            name,
            layout,
            this.getFilter(),
            manager,
            this.isIgnoreExceptions(),
            !bufferedIo || immediateFlush,
            this.getAdvertise() ? this.getConfiguration().getAdvertiser() : null
         );
      }
   }
}
