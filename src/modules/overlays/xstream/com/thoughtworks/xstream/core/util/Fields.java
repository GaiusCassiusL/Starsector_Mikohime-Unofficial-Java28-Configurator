package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Fields {
   public static Field locate(Class definedIn, Class fieldType, boolean isStatic) {
      Field field = null;

      try {
         Field[] fields = definedIn.getDeclaredFields();

         for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic(fields[i].getModifiers()) == isStatic && fieldType.isAssignableFrom(fields[i].getType())) {
               field = fields[i];
            }
         }

         if (field != null && !field.isAccessible()) {
            field.setAccessible(true);
         }
      } catch (SecurityException var6) {
      } catch (NoClassDefFoundError var7) {
      }

      return field;
   }

   public static Field find(Class type, String name) {
      try {
         Field result = type.getDeclaredField(name);
         if (!result.isAccessible()) {
            result.setAccessible(true);
         }

         return result;
      } catch (SecurityException e) {
         throw wrap("Cannot access field", type, name, e);
      } catch (NoSuchFieldException e) {
         throw wrap("Cannot access field", type, name, e);
      } catch (NoClassDefFoundError e) {
         throw wrap("Cannot access field", type, name, e);
      }
   }

   public static void write(Field field, Object instance, Object value) {
      try {
         field.set(instance, value);
      } catch (SecurityException e) {
         throw wrap("Cannot write field", field.getType(), field.getName(), e);
      } catch (IllegalArgumentException e) {
         throw wrap("Cannot write field", field.getType(), field.getName(), e);
      } catch (IllegalAccessException e) {
         throw wrap("Cannot write field", field.getType(), field.getName(), e);
      } catch (NoClassDefFoundError e) {
         throw wrap("Cannot write field", field.getType(), field.getName(), e);
      }
   }

   public static Object read(Field field, Object instance) {
      try {
         return field.get(instance);
      } catch (SecurityException e) {
         throw wrap("Cannot read field", field.getType(), field.getName(), e);
      } catch (IllegalArgumentException e) {
         throw wrap("Cannot read field", field.getType(), field.getName(), e);
      } catch (IllegalAccessException e) {
         throw wrap("Cannot read field", field.getType(), field.getName(), e);
      } catch (NoClassDefFoundError e) {
         throw wrap("Cannot read field", field.getType(), field.getName(), e);
      }
   }

   private static ObjectAccessException wrap(String message, Class type, String name, Throwable ex) {
      ObjectAccessException exception = new ObjectAccessException(message, ex);
      exception.add("field", type.getName() + "." + name);
      return exception;
   }
}
