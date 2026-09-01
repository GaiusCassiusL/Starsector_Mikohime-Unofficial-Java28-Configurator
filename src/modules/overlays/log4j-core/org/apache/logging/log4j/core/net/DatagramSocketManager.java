package org.apache.logging.log4j.core.net;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.util.Strings;

public class DatagramSocketManager extends AbstractSocketManager {
   private static final DatagramSocketManager.DatagramSocketManagerFactory FACTORY = new DatagramSocketManager.DatagramSocketManagerFactory();

   protected DatagramSocketManager(
      final String name, final OutputStream os, final InetAddress inetAddress, final String host, final int port, final Layout layout, final int bufferSize
   ) {
      super(name, os, inetAddress, host, port, layout, true, bufferSize);
   }

   public static DatagramSocketManager getSocketManager(final String host, final int port, final Layout layout, final int bufferSize) {
      if (Strings.isEmpty(host)) {
         throw new IllegalArgumentException("A host name is required");
      } else if (port <= 0) {
         throw new IllegalArgumentException("A port value is required");
      } else {
         return (DatagramSocketManager)getManager("UDP:" + host + ":" + port, new DatagramSocketManager.FactoryData(host, port, layout, bufferSize), FACTORY);
      }
   }

   @Override
   public Map<String, String> getContentFormat() {
      Map<String, String> result = new HashMap<>(super.getContentFormat());
      result.put("protocol", "udp");
      result.put("direction", "out");
      return result;
   }

   private static class DatagramSocketManagerFactory implements ManagerFactory<DatagramSocketManager, DatagramSocketManager.FactoryData> {
      public DatagramSocketManager createManager(final String name, final DatagramSocketManager.FactoryData data) {
         InetAddress inetAddress;
         try {
            inetAddress = InetAddress.getByName(data.host);
         } catch (UnknownHostException ex) {
            DatagramSocketManager.LOGGER.error("Could not find address of " + data.host, ex);
            return null;
         }

         OutputStream os = new DatagramOutputStream(data.host, data.port, data.layout.getHeader(), data.layout.getFooter());
         return new DatagramSocketManager(name, os, inetAddress, data.host, data.port, data.layout, data.bufferSize);
      }
   }

   private static class FactoryData {
      private final String host;
      private final int port;
      private final Layout layout;
      private final int bufferSize;

      public FactoryData(final String host, final int port, final Layout layout, final int bufferSize) {
         this.host = host;
         this.port = port;
         this.layout = layout;
         this.bufferSize = bufferSize;
      }
   }
}
