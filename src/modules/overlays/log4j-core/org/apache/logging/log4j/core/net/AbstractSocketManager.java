package org.apache.logging.log4j.core.net;

import java.io.OutputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.OutputStreamManager;

public abstract class AbstractSocketManager extends OutputStreamManager {
   protected final InetAddress inetAddress;
   protected final String host;
   protected final int port;

   public AbstractSocketManager(
      final String name,
      final OutputStream os,
      final InetAddress inetAddress,
      final String host,
      final int port,
      final Layout layout,
      final boolean writeHeader,
      final int bufferSize
   ) {
      super(os, name, layout, writeHeader, bufferSize);
      this.inetAddress = inetAddress;
      this.host = host;
      this.port = port;
   }

   @Override
   public Map<String, String> getContentFormat() {
      Map<String, String> result = new HashMap<>(super.getContentFormat());
      result.put("port", Integer.toString(this.port));
      result.put("address", this.inetAddress.getHostAddress());
      return result;
   }

   public String getHost() {
      return this.host;
   }

   public int getPort() {
      return this.port;
   }
}
