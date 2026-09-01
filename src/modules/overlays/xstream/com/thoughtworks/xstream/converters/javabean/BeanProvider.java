package com.thoughtworks.xstream.converters.javabean;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.ErrorWritingException;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class BeanProvider implements JavaBeanProvider {
   /** @deprecated */
   protected static final Object[] NO_PARAMS = new Object[0];
   protected PropertyDictionary propertyDictionary;

   public BeanProvider() {
      this(new PropertyDictionary(new NativePropertySorter()));
   }

   public BeanProvider(Comparator propertyNameComparator) {
      this(new PropertyDictionary(new ComparingPropertySorter(propertyNameComparator)));
   }

   public BeanProvider(PropertyDictionary propertyDictionary) {
      this.propertyDictionary = propertyDictionary;
   }

   public Object newInstance(Class type) {
      ErrorWritingException ex = null;
      if (type != void.class && type != Void.class) {
         try {
            return type.newInstance();
         } catch (InstantiationException e) {
            ex = new ConversionException("Cannot construct type", e);
         } catch (IllegalAccessException e) {
            ex = new ObjectAccessException("Cannot construct type", e);
         } catch (SecurityException e) {
            ex = new ObjectAccessException("Cannot construct type", e);
         } catch (ExceptionInInitializerError e) {
            ex = new ConversionException("Cannot construct type", e);
         }
      } else {
         ex = new ConversionException("Security alert: Marshalling rejected");
      }

      ex.add("construction-type", type.getName());
      throw ex;
   }

   public void visitSerializableProperties(Object object, JavaBeanProvider.Visitor visitor) {
      PropertyDescriptor[] propertyDescriptors = this.getSerializableProperties(object);

      for (int i = 0; i < propertyDescriptors.length; i++) {
         ErrorWritingException ex = null;
         PropertyDescriptor property = propertyDescriptors[i];

         try {
            Method readMethod = property.getReadMethod();
            String name = property.getName();
            Class definedIn = readMethod.getDeclaringClass();
            if (visitor.shouldVisit(name, definedIn)) {
               Object value = readMethod.invoke(object);
               visitor.visit(name, property.getPropertyType(), definedIn, value);
            }
         } catch (IllegalArgumentException e) {
            ex = new ConversionException("Cannot get property", e);
         } catch (IllegalAccessException e) {
            ex = new ObjectAccessException("Cannot access property", e);
         } catch (InvocationTargetException e) {
            ex = new ConversionException("Cannot get property", e.getTargetException());
         }

         if (ex != null) {
            ex.add("property", object.getClass() + "." + property.getName());
            throw ex;
         }
      }
   }

   public void writeProperty(Object object, String propertyName, Object value) {
      ErrorWritingException ex = null;
      PropertyDescriptor property = this.getProperty(propertyName, object.getClass());

      try {
         property.getWriteMethod().invoke(object, value);
      } catch (IllegalArgumentException e) {
         ex = new ConversionException("Cannot set property", e);
      } catch (IllegalAccessException e) {
         ex = new ObjectAccessException("Cannot access property", e);
      } catch (InvocationTargetException e) {
         ex = new ConversionException("Cannot set property", e.getTargetException());
      }

      if (ex != null) {
         ex.add("property", object.getClass() + "." + property.getName());
         throw ex;
      }
   }

   public Class getPropertyType(Object object, String name) {
      return this.getProperty(name, object.getClass()).getPropertyType();
   }

   public boolean propertyDefinedInClass(String name, Class type) {
      return this.propertyDictionary.propertyDescriptorOrNull(type, name) != null;
   }

   public boolean canInstantiate(Class type) {
      try {
         return type != null && this.newInstance(type) != null;
      } catch (ErrorWritingException e) {
         return false;
      }
   }

   /** @deprecated */
   protected Constructor getDefaultConstrutor(Class type) {
      Constructor[] constructors = type.getConstructors();

      for (int i = 0; i < constructors.length; i++) {
         Constructor c = constructors[i];
         if (c.getParameterTypes().length == 0 && Modifier.isPublic(c.getModifiers())) {
            return c;
         }
      }

      return null;
   }

   protected PropertyDescriptor[] getSerializableProperties(Object object) {
      List result = new ArrayList();
      Iterator iter = this.propertyDictionary.propertiesFor(object.getClass());

      while (iter.hasNext()) {
         PropertyDescriptor descriptor = (PropertyDescriptor)iter.next();
         if (this.canStreamProperty(descriptor)) {
            result.add(descriptor);
         }
      }

      return result.toArray(new PropertyDescriptor[result.size()]);
   }

   protected boolean canStreamProperty(PropertyDescriptor descriptor) {
      return descriptor.getReadMethod() != null && descriptor.getWriteMethod() != null;
   }

   public boolean propertyWriteable(String name, Class type) {
      PropertyDescriptor property = this.getProperty(name, type);
      return property.getWriteMethod() != null;
   }

   protected PropertyDescriptor getProperty(String name, Class type) {
      return this.propertyDictionary.propertyDescriptor(type, name);
   }

   /** @deprecated */
   public interface Visitor extends JavaBeanProvider.Visitor {
   }
}
