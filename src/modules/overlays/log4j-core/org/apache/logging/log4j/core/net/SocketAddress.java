package org.apache.logging.log4j.core.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.plugins.validation.constraints.ValidPort;

@Configurable(printObject = true)
@Plugin
public final class SocketAddress {
   private final InetSocketAddress socketAddress;

   public static SocketAddress getLoopback() {
      return new SocketAddress(InetAddress.getLoopbackAddress(), 0);
   }

   private SocketAddress(final InetAddress host, final int port) {
      this.socketAddress = new InetSocketAddress(host, port);
   }

   public InetSocketAddress getSocketAddress() {
      return this.socketAddress;
   }

   public int getPort() {
      return this.socketAddress.getPort();
   }

   public InetAddress getAddress() {
      return this.socketAddress.getAddress();
   }

   public String getHostName() {
      return this.socketAddress.getHostName();
   }

   @PluginFactory
   public static SocketAddress.Builder newBuilder() {
      return new SocketAddress.Builder();
   }

   @Override
   public String toString() {
      return this.socketAddress.toString();
   }

   public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<SocketAddress> {
      @PluginBuilderAttribute
      @ValidHost
      private InetAddress host;
      @PluginBuilderAttribute
      @ValidPort
      private int port;

      public SocketAddress.Builder setHost(final InetAddress host) {
         this.host = host;
         return this;
      }

      public SocketAddress.Builder setPort(final int port) {
         this.port = port;
         return this;
      }

      public SocketAddress build() {
         return new SocketAddress(this.host, this.port);
      }
   }
}
