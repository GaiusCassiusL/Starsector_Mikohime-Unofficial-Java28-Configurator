package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.core.util.Fields;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class AbstractAttributedCharacterIteratorAttributeConverter extends AbstractSingleValueConverter {
   private static final Map instanceMaps = Collections.synchronizedMap(new HashMap());
   private final Class type;

   public AbstractAttributedCharacterIteratorAttributeConverter(Class type) {
      if (!Attribute.class.isAssignableFrom(type)) {
         throw new IllegalArgumentException(type.getName() + " is not a " + Attribute.class.getName());
      }

      this.type = type;
   }

   public boolean canConvert(Class type) {
      return type == this.type && !this.getAttributeMap().isEmpty();
   }

   public String toString(Object source) {
      return this.getName((Attribute)source);
   }

   private String getName(Attribute attribute) {
      Exception ex = null;
      if (AbstractAttributedCharacterIteratorAttributeConverter.Reflections.getName != null) {
         try {
            return (String)AbstractAttributedCharacterIteratorAttributeConverter.Reflections.getName.invoke(attribute, (Object[])null);
         } catch (IllegalAccessException e) {
            ex = e;
         } catch (InvocationTargetException e) {
            ex = e;
         }
      }

      String s = attribute.toString();
      String className = attribute.getClass().getName();
      if (s.startsWith(className)) {
         return s.substring(className.length() + 1, s.length() - 1);
      }

      ConversionException exception = new ConversionException("Cannot find name of attribute", ex);
      exception.add("attribute-type", className);
      throw exception;
   }

   public Object fromString(String str) {
      Object attr = this.getAttributeMap().get(str);
      if (attr != null) {
         return attr;
      }

      ConversionException exception = new ConversionException("Cannot find attribute");
      exception.add("attribute-type", this.type.getName());
      exception.add("attribute-name", str);
      throw exception;
   }

   private Map getAttributeMap() {
      Map attributeMap = (Map)instanceMaps.get(this.type.getName());
      if (attributeMap == null) {
         attributeMap = this.buildAttributeMap();
         instanceMaps.put(this.type.getName(), attributeMap);
      }

      return attributeMap;
   }

   private Map buildAttributeMap() {
      Map attributeMap = new HashMap();
      Field instanceMap = Fields.locate(this.type, Map.class, true);
      if (instanceMap != null) {
         try {
            Map map = (Map)Fields.read(instanceMap, null);
            if (map != null) {
               boolean valid = true;
               Iterator iter = map.entrySet().iterator();

               while (valid && iter.hasNext()) {
                  Entry entry = (Entry)iter.next();
                  valid = entry.getKey().getClass() == String.class && entry.getValue().getClass() == this.type;
               }

               if (valid) {
                  attributeMap.putAll(map);
               }
            }
         } catch (ObjectAccessException var10) {
         }
      }

      if (attributeMap.isEmpty()) {
         try {
            Field[] fields = this.type.getDeclaredFields();

            for (int i = 0; i < fields.length; i++) {
               if (fields[i].getType() == this.type == Modifier.isStatic(fields[i].getModifiers())) {
                  Attribute attribute = (Attribute)Fields.read(fields[i], null);
                  attributeMap.put(this.toString(attribute), attribute);
               }
            }
         } catch (SecurityException e) {
            attributeMap.clear();
         } catch (ObjectAccessException e) {
            attributeMap.clear();
         } catch (NoClassDefFoundError e) {
            attributeMap.clear();
         }
      }

      return attributeMap;
   }

   private static class Reflections {
      private static final Method getName;

      static {
         Method method = null;

         try {
            method = Attribute.class.getDeclaredMethod("getName", (Class<?>[])null);
            if (!method.isAccessible()) {
               method.setAccessible(true);
            }
         } catch (SecurityException var2) {
         } catch (NoSuchMethodException var3) {
         }

         getName = method;
      }
   }
}
