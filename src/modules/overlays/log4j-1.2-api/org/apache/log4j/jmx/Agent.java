package org.apache.log4j.jmx;

import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.apache.log4j.Logger;

@Deprecated
public class Agent {
   @Deprecated
   static Logger log = Logger.getLogger(Agent.class);

   private static Object createServer() {
      Object newInstance = null;

      try {
         return Class.forName("com.sun.jdmk.comm.HtmlAdapterServer").newInstance();
      } catch (ClassNotFoundException ex) {
         throw new RuntimeException(ex.toString());
      } catch (InstantiationException ex) {
         throw new RuntimeException(ex.toString());
      } catch (IllegalAccessException ex) {
         throw new RuntimeException(ex.toString());
      }
   }

   private static void startServer(final Object server) {
      try {
         server.getClass().getMethod("start").invoke(server);
      } catch (InvocationTargetException ex) {
         Throwable cause = ex.getTargetException();
         if (cause instanceof RuntimeException) {
            throw (RuntimeException)cause;
         }

         if (cause == null) {
            throw new RuntimeException();
         }

         if (cause instanceof InterruptedException || cause instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         throw new RuntimeException(cause.toString());
      } catch (NoSuchMethodException ex) {
         throw new RuntimeException(ex.toString());
      } catch (IllegalAccessException ex) {
         throw new RuntimeException(ex.toString());
      }
   }

   @Deprecated
   public void start() {
      MBeanServer server = MBeanServerFactory.createMBeanServer();
      Object html = createServer();

      try {
         log.info("Registering HtmlAdaptorServer instance.");
         server.registerMBean(html, new ObjectName("Adaptor:name=html,port=8082"));
         log.info("Registering HierarchyDynamicMBean instance.");
         HierarchyDynamicMBean hdm = new HierarchyDynamicMBean();
         server.registerMBean(hdm, new ObjectName("log4j:hiearchy=default"));
      } catch (JMException e) {
         log.error("Problem while registering MBeans instances.", e);
         return;
      } catch (RuntimeException e) {
         log.error("Problem while registering MBeans instances.", e);
         return;
      }

      startServer(html);
   }
}
