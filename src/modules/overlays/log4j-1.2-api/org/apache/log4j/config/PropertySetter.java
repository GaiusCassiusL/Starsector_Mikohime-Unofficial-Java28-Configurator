package org.apache.log4j.config;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import org.apache.log4j.Appender;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.OptionHandler;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.OptionConverter;
import org.apache.logging.log4j.status.StatusLogger;

public class PropertySetter {
   private static final PropertyDescriptor[] EMPTY_PROPERTY_DESCRIPTOR_ARRAY = new PropertyDescriptor[0];
   private static final Logger LOGGER = StatusLogger.getLogger();
   protected Object obj;
   protected PropertyDescriptor[] props;

   public PropertySetter(final Object obj) {
      this.obj = obj;
   }

   public static void setProperties(final Object obj, final Properties properties, final String prefix) {
      new PropertySetter(obj).setProperties(properties, prefix);
   }

   protected void introspect() {
      try {
         BeanInfo bi = Introspector.getBeanInfo(this.obj.getClass());
         this.props = bi.getPropertyDescriptors();
      } catch (IntrospectionException ex) {
         LOGGER.error("Failed to introspect {}: {}", this.obj, ex.getMessage());
         this.props = EMPTY_PROPERTY_DESCRIPTOR_ARRAY;
      }
   }

   public void setProperties(final Properties properties, final String prefix) {
      int len = prefix.length();

      for (String key : properties.stringPropertyNames()) {
         if (key.startsWith(prefix) && key.indexOf(46, len + 1) <= 0) {
            String value = OptionConverter.findAndSubst(key, properties);
            key = key.substring(len);
            if (!"layout".equals(key) && !"errorhandler".equals(key) || !(this.obj instanceof Appender)) {
               PropertyDescriptor prop = this.getPropertyDescriptor(Introspector.decapitalize(key));
               if (prop != null && OptionHandler.class.isAssignableFrom(prop.getPropertyType()) && prop.getWriteMethod() != null) {
                  OptionHandler opt = (OptionHandler)OptionConverter.instantiateByKey(properties, prefix + key, prop.getPropertyType(), null);
                  PropertySetter setter = new PropertySetter(opt);
                  setter.setProperties(properties, prefix + key + ".");

                  try {
                     prop.getWriteMethod().invoke(this.obj, opt);
                  } catch (InvocationTargetException ex) {
                     if (ex.getTargetException() instanceof InterruptedException || ex.getTargetException() instanceof InterruptedIOException) {
                        Thread.currentThread().interrupt();
                     }

                     LOGGER.warn("Failed to set property [{}] to value \"{}\".", key, value, ex);
                  } catch (IllegalAccessException | RuntimeException ex) {
                     LOGGER.warn("Failed to set property [{}] to value \"{}\".", key, value, ex);
                  }
               } else {
                  this.setProperty(key, value);
               }
            }
         }
      }

      this.activate();
   }

   public void setProperty(String name, String value) {
      if (value != null) {
         name = Introspector.decapitalize(name);
         PropertyDescriptor prop = this.getPropertyDescriptor(name);
         if (prop == null) {
            LOGGER.warn("No such property [" + name + "] in " + this.obj.getClass().getName() + ".");
         } else {
            try {
               this.setProperty(prop, name, value);
            } catch (PropertySetterException ex) {
               LOGGER.warn("Failed to set property [{}] to value \"{}\".", name, value, ex.rootCause);
            }
         }
      }
   }

   public void setProperty(PropertyDescriptor prop, String name, String value) throws PropertySetterException {
      Method setter = prop.getWriteMethod();
      if (setter == null) {
         throw new PropertySetterException("No setter for property [" + name + "].");
      }

      Class<?>[] paramTypes = setter.getParameterTypes();
      if (paramTypes.length != 1) {
         throw new PropertySetterException("#params for setter != 1");
      }

      Object arg;
      try {
         arg = this.convertArg(value, paramTypes[0]);
      } catch (Throwable t) {
         throw new PropertySetterException("Conversion to type [" + paramTypes[0] + "] failed. Reason: " + t);
      }

      if (arg == null) {
         throw new PropertySetterException("Conversion to type [" + paramTypes[0] + "] failed.");
      }

      LOGGER.debug("Setting property [" + name + "] to [" + arg + "].");

      try {
         setter.invoke(this.obj, arg);
      } catch (InvocationTargetException ex) {
         if (ex.getTargetException() instanceof InterruptedException || ex.getTargetException() instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         throw new PropertySetterException(ex);
      } catch (IllegalAccessException | RuntimeException ex) {
         throw new PropertySetterException(ex);
      }
   }

   protected Object convertArg(final String val, final Class<?> type) {
      if (val == null) {
         return null;
      }

      String v = val.trim();
      if (String.class.isAssignableFrom(type)) {
         return val;
      }

      if (int.class.isAssignableFrom(type)) {
         return Integer.parseInt(v);
      }

      if (long.class.isAssignableFrom(type)) {
         return Long.parseLong(v);
      }

      if (boolean.class.isAssignableFrom(type)) {
         if ("true".equalsIgnoreCase(v)) {
            return Boolean.TRUE;
         }

         if ("false".equalsIgnoreCase(v)) {
            return Boolean.FALSE;
         }
      } else {
         if (Priority.class.isAssignableFrom(type)) {
            return org.apache.log4j.helpers.OptionConverter.toLevel(v, Log4j1Configuration.DEFAULT_LEVEL);
         }

         if (ErrorHandler.class.isAssignableFrom(type)) {
            return OptionConverter.instantiateByClassName(v, ErrorHandler.class, null);
         }
      }

      return null;
   }

   protected PropertyDescriptor getPropertyDescriptor(String name) {
      if (this.props == null) {
         this.introspect();
      }

      for (PropertyDescriptor prop : this.props) {
         if (name.equals(prop.getName())) {
            return prop;
         }
      }

      return null;
   }

   public void activate() {
      if (this.obj instanceof OptionHandler) {
         ((OptionHandler)this.obj).activateOptions();
      }
   }
}
