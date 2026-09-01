package org.apache.logging.log4j.core.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.util.Strings;

public class SslSocketManager extends TcpSocketManager {
   public static final int DEFAULT_PORT = 6514;
   private static final SslSocketManager.SslSocketManagerFactory FACTORY = new SslSocketManager.SslSocketManagerFactory();
   private final SslConfiguration sslConfig;

   public SslSocketManager(
      final String name,
      final OutputStream os,
      final Socket sock,
      final SslConfiguration sslConfig,
      final InetAddress inetAddress,
      final String host,
      final int port,
      final int connectTimeoutMillis,
      final int reconnectionDelayMillis,
      final boolean immediateFail,
      final Layout layout,
      final int bufferSize,
      final SocketOptions socketOptions
   ) {
      super(name, os, sock, inetAddress, host, port, connectTimeoutMillis, reconnectionDelayMillis, immediateFail, layout, bufferSize, socketOptions);
      this.sslConfig = sslConfig;
   }

   public static SslSocketManager getSocketManager(
      final SslConfiguration sslConfig,
      final String host,
      int port,
      final int connectTimeoutMillis,
      int reconnectDelayMillis,
      final boolean immediateFail,
      final Layout layout,
      final int bufferSize,
      final SocketOptions socketOptions
   ) {
      if (Strings.isEmpty(host)) {
         throw new IllegalArgumentException("A host name is required");
      }

      if (port <= 0) {
         port = 6514;
      }

      if (reconnectDelayMillis == 0) {
         reconnectDelayMillis = 30000;
      }

      String name = "TLS:" + host + ":" + port;
      return (SslSocketManager)getManager(
         name,
         new SslSocketManager.SslFactoryData(
            sslConfig, host, port, connectTimeoutMillis, reconnectDelayMillis, immediateFail, layout, bufferSize, socketOptions
         ),
         FACTORY
      );
   }

   @Override
   protected Socket createSocket(final InetSocketAddress socketAddress) throws IOException {
      SSLSocketFactory socketFactory = createSslSocketFactory(this.sslConfig);
      Socket newSocket = socketFactory.createSocket();
      newSocket.connect(socketAddress, this.getConnectTimeoutMillis());
      return newSocket;
   }

   private static SSLSocketFactory createSslSocketFactory(final SslConfiguration sslConf) {
      SSLSocketFactory socketFactory;
      if (sslConf != null) {
         socketFactory = sslConf.getSslSocketFactory();
      } else {
         socketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
      }

      return socketFactory;
   }

   static Socket createSocket(
      final InetSocketAddress socketAddress, final int connectTimeoutMillis, final SslConfiguration sslConfiguration, final SocketOptions socketOptions
   ) throws IOException {
      SSLSocketFactory socketFactory = createSslSocketFactory(sslConfiguration);
      SSLSocket socket = (SSLSocket)socketFactory.createSocket();
      if (socketOptions != null) {
         socketOptions.apply(socket);
      }

      socket.connect(socketAddress, connectTimeoutMillis);
      if (socketOptions != null) {
         socketOptions.apply(socket);
      }

      return socket;
   }

   private static class SslFactoryData extends TcpSocketManager.FactoryData {
      protected SslConfiguration sslConfiguration;

      public SslFactoryData(
         final SslConfiguration sslConfiguration,
         final String host,
         final int port,
         final int connectTimeoutMillis,
         final int reconnectDelayMillis,
         final boolean immediateFail,
         final Layout layout,
         final int bufferSize,
         final SocketOptions socketOptions
      ) {
         super(host, port, connectTimeoutMillis, reconnectDelayMillis, immediateFail, layout, bufferSize, socketOptions);
         this.sslConfiguration = sslConfiguration;
      }

      @Override
      public String toString() {
         return "SslFactoryData [sslConfiguration="
            + this.sslConfiguration
            + ", host="
            + this.host
            + ", port="
            + this.port
            + ", connectTimeoutMillis="
            + this.connectTimeoutMillis
            + ", reconnectDelayMillis="
            + this.reconnectDelayMillis
            + ", immediateFail="
            + this.immediateFail
            + ", layout="
            + this.layout
            + ", bufferSize="
            + this.bufferSize
            + ", socketOptions="
            + this.socketOptions
            + "]";
      }
   }

   private static class SslSocketManagerFactory extends TcpSocketManager.TcpSocketManagerFactory<SslSocketManager, SslSocketManager.SslFactoryData> {
      SslSocketManager createManager(
         final String name, final OutputStream os, final Socket socket, final InetAddress inetAddress, final SslSocketManager.SslFactoryData data
      ) {
         return new SslSocketManager(
            name,
            os,
            socket,
            data.sslConfiguration,
            inetAddress,
            data.host,
            data.port,
            data.connectTimeoutMillis,
            data.reconnectDelayMillis,
            data.immediateFail,
            data.layout,
            data.bufferSize,
            data.socketOptions
         );
      }

      Socket createSocket(final SslSocketManager.SslFactoryData data) throws IOException {
         List<InetSocketAddress> socketAddresses = RESOLVER.resolveHost(data.host, data.port);
         IOException ioe = null;

         for (InetSocketAddress socketAddress : socketAddresses) {
            try {
               return SslSocketManager.createSocket(socketAddress, data.connectTimeoutMillis, data.sslConfiguration, data.socketOptions);
            } catch (IOException ex) {
               ioe = ex;
            }
         }

         throw new IOException(this.errorMessage(data, socketAddresses), ioe);
      }
   }
}
