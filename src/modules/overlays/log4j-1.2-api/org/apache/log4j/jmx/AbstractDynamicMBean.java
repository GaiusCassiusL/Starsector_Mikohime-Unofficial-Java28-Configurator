package org.apache.log4j.jmx;

import java.util.Enumeration;
import java.util.Vector;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

public abstract class AbstractDynamicMBean implements DynamicMBean, MBeanRegistration {
   String dClassName;
   MBeanServer server;
   private final Vector mbeanList = new Vector();

   protected static String getAppenderName(final Appender appender) {
      String name = appender.getName();
      if (name == null || name.trim().length() == 0) {
         name = appender.toString();
      }

      return name;
   }

   @Override
   public AttributeList getAttributes(final String[] attributeNames) {
      if (attributeNames == null) {
         throw new RuntimeOperationsException(new IllegalArgumentException("attributeNames[] cannot be null"), "Cannot invoke a getter of " + this.dClassName);
      }

      AttributeList resultList = new AttributeList();
      if (attributeNames.length == 0) {
         return resultList;
      }

      for (String attributeName : attributeNames) {
         try {
            Object value = this.getAttribute(attributeName);
            resultList.add(new Attribute(attributeName, value));
         } catch (JMException e) {
            e.printStackTrace();
         } catch (RuntimeException e) {
            e.printStackTrace();
         }
      }

      return resultList;
   }

   protected abstract Logger getLogger();

   @Override
   public void postDeregister() {
      this.getLogger().debug("postDeregister is called.");
   }

   @Override
   public void postRegister(final Boolean registrationDone) {
   }

   @Override
   public void preDeregister() {
      this.getLogger().debug("preDeregister called.");
      Enumeration iterator = this.mbeanList.elements();

      while (iterator.hasMoreElements()) {
         ObjectName name = (ObjectName)iterator.nextElement();

         try {
            this.server.unregisterMBean(name);
         } catch (InstanceNotFoundException e) {
            this.getLogger().warn("Missing MBean " + name.getCanonicalName());
         } catch (MBeanRegistrationException e) {
            this.getLogger().warn("Failed unregistering " + name.getCanonicalName());
         }
      }
   }

   @Override
   public ObjectName preRegister(final MBeanServer server, final ObjectName name) {
      this.getLogger().debug("preRegister called. Server=" + server + ", name=" + name);
      this.server = server;
      return name;
   }

   protected void registerMBean(final Object mbean, final ObjectName objectName) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
      this.server.registerMBean(mbean, objectName);
      this.mbeanList.add(objectName);
   }

   @Override
   public AttributeList setAttributes(final AttributeList attributes) {
      if (attributes == null) {
         throw new RuntimeOperationsException(
            new IllegalArgumentException("AttributeList attributes cannot be null"), "Cannot invoke a setter of " + this.dClassName
         );
      }

      AttributeList resultList = new AttributeList();
      if (attributes.isEmpty()) {
         return resultList;
      }

      for (Object attribute : attributes) {
         Attribute attr = (Attribute)attribute;

         try {
            this.setAttribute(attr);
            String name = attr.getName();
            Object value = this.getAttribute(name);
            resultList.add(new Attribute(name, value));
         } catch (JMException e) {
            e.printStackTrace();
         } catch (RuntimeException e) {
            e.printStackTrace();
         }
      }

      return resultList;
   }
}
