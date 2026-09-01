package org.apache.log4j.jmx;

import java.lang.reflect.Constructor;
import java.util.Vector;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerRepository;

public class HierarchyDynamicMBean extends AbstractDynamicMBean implements HierarchyEventListener, NotificationBroadcaster {
   static final String ADD_APPENDER = "addAppender.";
   static final String THRESHOLD = "threshold";
   private static final Logger log = Logger.getLogger(HierarchyDynamicMBean.class);
   private final MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[1];
   private final MBeanOperationInfo[] dOperations = new MBeanOperationInfo[1];
   private final Vector vAttributes = new Vector();
   private final String dClassName = this.getClass().getName();
   private final String dDescription = "This MBean acts as a management facade for org.apache.log4j.Hierarchy.";
   private final NotificationBroadcasterSupport nbs = new NotificationBroadcasterSupport();
   private final LoggerRepository hierarchy = LogManager.getLoggerRepository();

   public HierarchyDynamicMBean() {
      this.buildDynamicMBeanInfo();
   }

   @Override
   public void addAppenderEvent(final Category logger, final Appender appender) {
      log.debug("addAppenderEvent called: logger=" + logger.getName() + ", appender=" + appender.getName());
      Notification n = new Notification("addAppender." + logger.getName(), this, 0L);
      n.setUserData(appender);
      log.debug("sending notification.");
      this.nbs.sendNotification(n);
   }

   ObjectName addLoggerMBean(final Logger logger) {
      String name = logger.getName();
      ObjectName objectName = null;

      try {
         LoggerDynamicMBean loggerMBean = new LoggerDynamicMBean(logger);
         objectName = new ObjectName("log4j", "logger", name);
         if (!this.server.isRegistered(objectName)) {
            this.registerMBean(loggerMBean, objectName);
            NotificationFilterSupport nfs = new NotificationFilterSupport();
            nfs.enableType("addAppender." + logger.getName());
            log.debug("---Adding logger [" + name + "] as listener.");
            this.nbs.addNotificationListener(loggerMBean, nfs, null);
            this.vAttributes.add(new MBeanAttributeInfo("logger=" + name, "javax.management.ObjectName", "The " + name + " logger.", true, true, false));
         }
      } catch (JMException e) {
         log.error("Could not add loggerMBean for [" + name + "].", e);
      } catch (RuntimeException e) {
         log.error("Could not add loggerMBean for [" + name + "].", e);
      }

      return objectName;
   }

   public ObjectName addLoggerMBean(final String name) {
      Logger cat = LogManager.exists(name);
      return cat != null ? this.addLoggerMBean(cat) : null;
   }

   @Override
   public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
      this.nbs.addNotificationListener(listener, filter, handback);
   }

   private void buildDynamicMBeanInfo() {
      Constructor[] constructors = this.getClass().getConstructors();
      this.dConstructors[0] = new MBeanConstructorInfo("HierarchyDynamicMBean(): Constructs a HierarchyDynamicMBean instance", constructors[0]);
      this.vAttributes.add(new MBeanAttributeInfo("threshold", "java.lang.String", "The \"threshold\" state of the hiearchy.", true, true, false));
      MBeanParameterInfo[] params = new MBeanParameterInfo[]{new MBeanParameterInfo("name", "java.lang.String", "Create a logger MBean")};
      this.dOperations[0] = new MBeanOperationInfo("addLoggerMBean", "addLoggerMBean(): add a loggerMBean", params, "javax.management.ObjectName", 1);
   }

   @Override
   public Object getAttribute(final String attributeName) throws AttributeNotFoundException, MBeanException, ReflectionException {
      if (attributeName == null) {
         throw new RuntimeOperationsException(
            new IllegalArgumentException("Attribute name cannot be null"), "Cannot invoke a getter of " + this.dClassName + " with null attribute name"
         );
      }

      log.debug("Called getAttribute with [" + attributeName + "].");
      if (attributeName.equals("threshold")) {
         return this.hierarchy.getThreshold();
      }

      if (attributeName.startsWith("logger")) {
         int k = attributeName.indexOf("%3D");
         String val = attributeName;
         if (k > 0) {
            val = attributeName.substring(0, k) + "=" + attributeName.substring(k + 3);
         }

         try {
            return new ObjectName("log4j:" + val);
         } catch (JMException e) {
            log.error("Could not create ObjectName" + val);
         } catch (RuntimeException e) {
            log.error("Could not create ObjectName" + val);
         }
      }

      throw new AttributeNotFoundException("Cannot find " + attributeName + " attribute in " + this.dClassName);
   }

   @Override
   protected Logger getLogger() {
      return log;
   }

   @Override
   public MBeanInfo getMBeanInfo() {
      MBeanAttributeInfo[] attribs = new MBeanAttributeInfo[this.vAttributes.size()];
      this.vAttributes.toArray(attribs);
      return new MBeanInfo(
         this.dClassName,
         "This MBean acts as a management facade for org.apache.log4j.Hierarchy.",
         attribs,
         this.dConstructors,
         this.dOperations,
         new MBeanNotificationInfo[0]
      );
   }

   @Override
   public MBeanNotificationInfo[] getNotificationInfo() {
      return this.nbs.getNotificationInfo();
   }

   @Override
   public Object invoke(final String operationName, final Object[] params, final String[] signature) throws MBeanException, ReflectionException {
      if (operationName == null) {
         throw new RuntimeOperationsException(
            new IllegalArgumentException("Operation name cannot be null"), "Cannot invoke a null operation in " + this.dClassName
         );
      } else if (operationName.equals("addLoggerMBean")) {
         return this.addLoggerMBean((String)params[0]);
      } else {
         throw new ReflectionException(new NoSuchMethodException(operationName), "Cannot find the operation " + operationName + " in " + this.dClassName);
      }
   }

   @Override
   public void postRegister(final Boolean registrationDone) {
      log.debug("postRegister is called.");
      this.hierarchy.addHierarchyEventListener(this);
      Logger root = this.hierarchy.getRootLogger();
      this.addLoggerMBean(root);
   }

   @Override
   public void removeAppenderEvent(final Category cat, final Appender appender) {
      log.debug("removeAppenderCalled: logger=" + cat.getName() + ", appender=" + appender.getName());
   }

   @Override
   public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
      this.nbs.removeNotificationListener(listener);
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

      if (name.equals("threshold")) {
         Level l = OptionConverter.toLevel((String)value, this.hierarchy.getThreshold());
         this.hierarchy.setThreshold(l);
      }
   }
}
