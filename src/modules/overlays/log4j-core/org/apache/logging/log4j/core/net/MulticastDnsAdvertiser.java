package org.apache.logging.log4j.core.net;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

@Configurable(elementType = "advertiser")
@Plugin("MulticastDns")
public class MulticastDnsAdvertiser implements Advertiser {
   protected static final Logger LOGGER = StatusLogger.getLogger();
   private static final int MAX_LENGTH = 255;
   private static final int DEFAULT_PORT = 4555;
   private static final Object jmDNS = initializeJmDns();
   private static Class<?> jmDNSClass;
   private static Class<?> serviceInfoClass;

   @Override
   public Object advertise(final Map<String, String> properties) {
      Map<String, String> truncatedProperties = new HashMap<>();

      for (Entry<String, String> entry : properties.entrySet()) {
         if (entry.getKey().length() <= 255 && entry.getValue().length() <= 255) {
            truncatedProperties.put(entry.getKey(), entry.getValue());
         }
      }

      String protocol = truncatedProperties.get("protocol");
      String zone = "._log4j._" + (protocol != null ? protocol : "tcp") + ".local.";
      String portString = truncatedProperties.get("port");
      int port = Integers.parseInt(portString, 4555);
      String name = truncatedProperties.get("name");
      if (jmDNS != null) {
         boolean isVersion3 = false;

         try {
            jmDNSClass.getMethod("create");
            isVersion3 = true;
         } catch (NoSuchMethodException var13) {
         }

         Object serviceInfo;
         if (isVersion3) {
            serviceInfo = buildServiceInfoVersion3(zone, port, name, truncatedProperties);
         } else {
            serviceInfo = buildServiceInfoVersion1(zone, port, name, truncatedProperties);
         }

         try {
            Method method = jmDNSClass.getMethod("registerService", serviceInfoClass);
            method.invoke(jmDNS, serviceInfo);
         } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Unable to invoke registerService method", e);
         } catch (NoSuchMethodException e) {
            LOGGER.warn("No registerService method", e);
         }

         return serviceInfo;
      } else {
         LOGGER.warn("JMDNS not available - will not advertise ZeroConf support");
         return null;
      }
   }

   @Override
   public void unadvertise(final Object serviceInfo) {
      if (jmDNS != null) {
         try {
            Method method = jmDNSClass.getMethod("unregisterService", serviceInfoClass);
            method.invoke(jmDNS, serviceInfo);
         } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Unable to invoke unregisterService method", e);
         } catch (NoSuchMethodException e) {
            LOGGER.warn("No unregisterService method", e);
         }
      }
   }

   private static Object createJmDnsVersion1() {
      try {
         return jmDNSClass.getConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
         LOGGER.warn("Unable to instantiate JMDNS", e);
         return null;
      }
   }

   private static Object createJmDnsVersion3() {
      try {
         Method jmDNSCreateMethod = jmDNSClass.getMethod("create");
         return jmDNSCreateMethod.invoke(null, (Object[])null);
      } catch (IllegalAccessException | InvocationTargetException e) {
         LOGGER.warn("Unable to invoke create method", e);
      } catch (NoSuchMethodException e) {
         LOGGER.warn("Unable to get create method", e);
      }

      return null;
   }

   private static Object buildServiceInfoVersion1(final String zone, final int port, final String name, final Map<String, String> properties) {
      Hashtable<String, String> hashtableProperties = new Hashtable<>(properties);

      try {
         return serviceInfoClass.getConstructor(String.class, String.class, int.class, int.class, int.class, Hashtable.class)
            .newInstance(zone, name, port, 0, 0, hashtableProperties);
      } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
         LOGGER.warn("Unable to construct ServiceInfo instance", e);
      } catch (NoSuchMethodException e) {
         LOGGER.warn("Unable to get ServiceInfo constructor", e);
      }

      return null;
   }

   private static Object buildServiceInfoVersion3(final String zone, final int port, final String name, final Map<String, String> properties) {
      try {
         return serviceInfoClass.getMethod("create", String.class, String.class, int.class, int.class, int.class, Map.class)
            .invoke(null, zone, name, port, 0, 0, properties);
      } catch (IllegalAccessException | InvocationTargetException e) {
         LOGGER.warn("Unable to invoke create method", e);
      } catch (NoSuchMethodException e) {
         LOGGER.warn("Unable to find create method", e);
      }

      return null;
   }

   private static Object initializeJmDns() {
      try {
         jmDNSClass = LoaderUtil.loadClass("javax.jmdns.JmDNS");
         serviceInfoClass = LoaderUtil.loadClass("javax.jmdns.ServiceInfo");
         boolean isVersion3 = false;

         try {
            jmDNSClass.getMethod("create");
            isVersion3 = true;
         } catch (NoSuchMethodException var2) {
         }

         return isVersion3 ? createJmDnsVersion3() : createJmDnsVersion1();
      } catch (ClassNotFoundException | ExceptionInInitializerError e) {
         LOGGER.warn("JmDNS or serviceInfo class not found", e);
         return null;
      }
   }
}
