package org.apache.log4j.jmx;

import java.beans.IntrospectionException;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Vector;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;

public class LoggerDynamicMBean extends AbstractDynamicMBean implements NotificationListener {
   private static final Logger cat = Logger.getLogger(LoggerDynamicMBean.class);
   private final MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[1];
   private final MBeanOperationInfo[] dOperations = new MBeanOperationInfo[1];
   private final Vector dAttributes = new Vector();
   private final String dClassName = this.getClass().getName();
   private final String dDescription = "This MBean acts as a management facade for a org.apache.log4j.Logger instance.";
   private final Logger logger;

   public LoggerDynamicMBean(final Logger logger) {
      this.logger = logger;
      this.buildDynamicMBeanInfo();
   }

   void addAppender(final String appenderClass, final String appenderName) {
      cat.debug("addAppender called with " + appenderClass + ", " + appenderName);
      Appender appender = (Appender)OptionConverter.instantiateByClassName(appenderClass, Appender.class, null);
      appender.setName(appenderName);
      this.logger.addAppender(appender);
   }

   void appenderMBeanRegistration() {
      Enumeration enumeration = this.logger.getAllAppenders();

      while (enumeration.hasMoreElements()) {
         Appender appender = (Appender)enumeration.nextElement();
         this.registerAppenderMBean(appender);
      }
   }

   private void buildDynamicMBeanInfo() {
      Constructor[] constructors = this.getClass().getConstructors();
      this.dConstructors[0] = new MBeanConstructorInfo("HierarchyDynamicMBean(): Constructs a HierarchyDynamicMBean instance", constructors[0]);
      this.dAttributes.add(new MBeanAttributeInfo("name", "java.lang.String", "The name of this Logger.", true, false, false));
      this.dAttributes.add(new MBeanAttributeInfo("priority", "java.lang.String", "The priority of this logger.", true, true, false));
      MBeanParameterInfo[] params = new MBeanParameterInfo[]{
         new MBeanParameterInfo("class name", "java.lang.String", "add an appender to this logger"),
         new MBeanParameterInfo("appender name", "java.lang.String", "name of the appender")
      };
      this.dOperations[0] = new MBeanOperationInfo("addAppender", "addAppender(): add an appender", params, "void", 1);
   }

   @Override
   public Object getAttribute(final String attributeName) throws AttributeNotFoundException, MBeanException, ReflectionException {
      if (attributeName == null) {
         throw new RuntimeOperationsException(
            new IllegalArgumentException("Attribute name cannot be null"), "Cannot invoke a getter of " + this.dClassName + " with null attribute name"
         );
      }

      if (attributeName.equals("name")) {
         return this.logger.getName();
      }

      if (attributeName.equals("priority")) {
         Level l = this.logger.getLevel();
         return l == null ? null : l.toString();
      }

      if (attributeName.startsWith("appender=")) {
         try {
            return new ObjectName("log4j:" + attributeName);
         } catch (MalformedObjectNameException e) {
            cat.error("Could not create ObjectName" + attributeName);
         } catch (RuntimeException e) {
            cat.error("Could not create ObjectName" + attributeName);
         }
      }

      throw new AttributeNotFoundException("Cannot find " + attributeName + " attribute in " + this.dClassName);
   }

   @Override
   protected Logger getLogger() {
      return this.logger;
   }

   @Override
   public MBeanInfo getMBeanInfo() {
      MBeanAttributeInfo[] attribs = new MBeanAttributeInfo[this.dAttributes.size()];
      this.dAttributes.toArray(attribs);
      return new MBeanInfo(
         this.dClassName,
         "This MBean acts as a management facade for a org.apache.log4j.Logger instance.",
         attribs,
         this.dConstructors,
         this.dOperations,
         new MBeanNotificationInfo[0]
      );
   }

   @Override
   public void handleNotification(final Notification notification, final Object handback) {
      cat.debug("Received notification: " + notification.getType());
      this.registerAppenderMBean((Appender)notification.getUserData());
   }

   @Override
   public Object invoke(final String operationName, final Object[] params, final String[] signature) throws MBeanException, ReflectionException {
      if (operationName.equals("addAppender")) {
         this.addAppender((String)params[0], (String)params[1]);
         return "Hello world.";
      } else {
         return null;
      }
   }

   @Override
   public void postRegister(final Boolean registrationDone) {
      this.appenderMBeanRegistration();
   }

   void registerAppenderMBean(final Appender appender) {
      String name = getAppenderName(appender);
      cat.debug("Adding AppenderMBean for appender named " + name);
      ObjectName objectName = null;

      try {
         AppenderDynamicMBean appenderMBean = new AppenderDynamicMBean(appender);
         objectName = new ObjectName("log4j", "appender", name);
         if (!this.server.isRegistered(objectName)) {
            this.registerMBean(appenderMBean, objectName);
            this.dAttributes.add(new MBeanAttributeInfo("appender=" + name, "javax.management.ObjectName", "The " + name + " appender.", true, true, false));
         }
      } catch (JMException e) {
         cat.error("Could not add appenderMBean for [" + name + "].", e);
      } catch (IntrospectionException e) {
         cat.error("Could not add appenderMBean for [" + name + "].", e);
      } catch (RuntimeException e) {
         cat.error("Could not add appenderMBean for [" + name + "].", e);
      }
   }

   @Override
   public void setAttribute(final Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
      if (attribute == null) {
         throw new RuntimeOperationsException(
            new IllegalArgumentException("Attribute cannot be null"), "Cannot invoke a setter of " + this.dClassName + " with null attribute"
         );
      }

      String name = attribute.getName();
      Object value = attribute.getValue();
      if (name == null) {
         throw new RuntimeOperationsException(
            new IllegalArgumentException("Attribute name cannot be null"), "Cannot invoke the setter of " + this.dClassName + " with null attribute name"
         );
      }

      if (name.equals("priority")) {
         if (value instanceof String) {
            String s = (String)value;
            Level p = this.logger.getLevel();
            if (s.equalsIgnoreCase("NULL")) {
               p = null;
            } else {
               p = OptionConverter.toLevel(s, p);
            }

            this.logger.setLevel(p);
         }
      } else {
         throw new AttributeNotFoundException("Attribute " + name + " not found in " + this.getClass().getName());
      }
   }
}
