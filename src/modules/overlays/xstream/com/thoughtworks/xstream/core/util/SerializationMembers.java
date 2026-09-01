package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.ErrorWritingException;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.core.Caching;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SerializationMembers implements Caching {
   private static final Method NO_METHOD = SerializationMembers.NO_METHOD_MARKER.class.getDeclaredMethods()[0];
   private static final Object[] EMPTY_ARGS = new Object[0];
   private static final Class[] EMPTY_CLASSES = new Class[0];
   private static final ObjectStreamField[] NO_FIELDS = new ObjectStreamField[0];
   private static final int PERSISTENT_FIELDS_MODIFIER = 26;
   private static final String[] OBJECT_TYPE_FIELDS = new String[]{"readResolve", "writeReplace", "readObject", "writeObject"};
   private final MemberStore declaredCache = MemberStore.newSynchronizedInstance();
   private final MemberStore resRepCache = MemberStore.newSynchronizedInstance();
   private final Map fieldCache = Collections.synchronizedMap(new HashMap());

   public SerializationMembers() {
      for (int i = 0; i < OBJECT_TYPE_FIELDS.length; i++) {
         this.declaredCache.put(Object.class, OBJECT_TYPE_FIELDS[i], NO_METHOD);
      }

      for (int i = 0; i < 2; i++) {
         this.resRepCache.put(Object.class, OBJECT_TYPE_FIELDS[i], NO_METHOD);
      }
   }

   public Object callReadResolve(Object result) {
      if (result == null) {
         return null;
      }

      Class resultType = result.getClass();
      Method readResolveMethod = this.getRRMethod(resultType, "readResolve");
      if (readResolveMethod != null) {
         ErrorWritingException ex = null;

         try {
            return readResolveMethod.invoke(result, EMPTY_ARGS);
         } catch (IllegalAccessException e) {
            ex = new ObjectAccessException("Cannot access method", e);
         } catch (InvocationTargetException e) {
            ex = new ConversionException("Failed calling method", e.getTargetException());
         }

         ex.add("method", resultType.getName() + ".readResolve()");
         throw ex;
      } else {
         return result;
      }
   }

   public Object callWriteReplace(Object object) {
      if (object == null) {
         return null;
      }

      Class objectType = object.getClass();
      Method writeReplaceMethod = this.getRRMethod(objectType, "writeReplace");
      if (writeReplaceMethod != null) {
         ErrorWritingException ex = null;

         try {
            Object replaced = writeReplaceMethod.invoke(object, EMPTY_ARGS);
            if (replaced != null && !object.getClass().equals(replaced.getClass())) {
               replaced = this.callWriteReplace(replaced);
            }

            return replaced;
         } catch (IllegalAccessException e) {
            ex = new ObjectAccessException("Cannot access method", e);
         } catch (InvocationTargetException e) {
            ex = new ConversionException("Failed calling method", e.getTargetException());
         } catch (ErrorWritingException e) {
            ex = e;
         }

         ex.add("method", objectType.getName() + ".writeReplace()");
         throw ex;
      } else {
         return object;
      }
   }

   public boolean supportsReadObject(Class type, boolean includeBaseClasses) {
      return this.getMethod(type, "readObject", new Class[]{ObjectInputStream.class}, includeBaseClasses) != null;
   }

   public void callReadObject(Class type, Object object, ObjectInputStream stream) {
      ErrorWritingException ex = null;

      try {
         Method readObjectMethod = this.getMethod(type, "readObject", new Class[]{ObjectInputStream.class}, false);
         readObjectMethod.invoke(object, stream);
      } catch (IllegalAccessException e) {
         ex = new ObjectAccessException("Cannot access method", e);
      } catch (InvocationTargetException e) {
         ex = new ConversionException("Failed calling method", e.getTargetException());
      }

      if (ex != null) {
         ex.add("method", object.getClass().getName() + ".readObject()");
         throw ex;
      }
   }

   public boolean supportsWriteObject(Class type, boolean includeBaseClasses) {
      return this.getMethod(type, "writeObject", new Class[]{ObjectOutputStream.class}, includeBaseClasses) != null;
   }

   public void callWriteObject(Class type, Object instance, ObjectOutputStream stream) {
      ErrorWritingException ex = null;

      try {
         Method readObjectMethod = this.getMethod(type, "writeObject", new Class[]{ObjectOutputStream.class}, false);
         readObjectMethod.invoke(instance, stream);
      } catch (IllegalAccessException e) {
         ex = new ObjectAccessException("Cannot access method", e);
      } catch (InvocationTargetException e) {
         Throwable cause = e.getTargetException();
         if (cause instanceof ConversionException) {
            throw (ConversionException)cause;
         }

         ex = new ConversionException("Failed calling method", e.getTargetException());
      }

      if (ex != null) {
         ex.add("method", instance.getClass().getName() + ".writeObject()");
         throw ex;
      }
   }

   private Method getMethod(Class type, String name, Class[] parameterTypes, boolean includeBaseclasses) {
      Method method = this.getMethod(type, name, parameterTypes);
      return method != NO_METHOD && (includeBaseclasses || method.getDeclaringClass().equals(type)) ? method : null;
   }

   private Method getMethod(Class type, String name, Class[] parameterTypes) {
      if (type == null) {
         return null;
      }

      Method result = (Method)this.declaredCache.get(type, name);
      if (result == null) {
         try {
            result = type.getDeclaredMethod(name, parameterTypes);
            if (!result.isAccessible()) {
               result.setAccessible(true);
            }
         } catch (NoSuchMethodException e) {
            result = this.getMethod(type.getSuperclass(), name, parameterTypes);
         }

         this.declaredCache.put(type, name, result);
      }

      return result;
   }

   private Method getRRMethod(Class type, String name) {
      Method result = (Method)this.resRepCache.get(type, name);
      if (result == null) {
         result = this.getMethod(type, name, EMPTY_CLASSES, true);
         if (result != null && result.getDeclaringClass() != type) {
            if ((result.getModifiers() & 5) == 0 && ((result.getModifiers() & 2) > 0 || type.getPackage() != result.getDeclaringClass().getPackage())) {
               result = NO_METHOD;
            }
         } else if (result == null) {
            result = NO_METHOD;
         }

         this.resRepCache.put(type, name, result);
      }

      return result == NO_METHOD ? null : result;
   }

   public boolean hasSerializablePersistentFields(Class type) {
      if (type == null) {
         return false;
      }

      ObjectStreamField[] result = (ObjectStreamField[])this.fieldCache.get(type.getName());
      if (result == null) {
         ErrorWritingException ex = null;

         try {
            Field field = type.getDeclaredField("serialPersistentFields");
            if ((field.getModifiers() & 26) == 26) {
               field.setAccessible(true);
               result = (ObjectStreamField[])field.get(null);
            }
         } catch (NoSuchFieldException var5) {
         } catch (IllegalAccessException e) {
            ex = new ObjectAccessException("Cannot get field", e);
         } catch (ClassCastException e) {
            ex = new ConversionException("Incompatible field type", e);
         }

         if (ex != null) {
            ex.add("field", type.getName() + ".serialPersistentFields");
            throw ex;
         }

         if (result == null) {
            result = NO_FIELDS;
         }

         this.fieldCache.put(type.getName(), result);
      }

      return result != NO_FIELDS;
   }

   public void flushCache() {
      Set classNames = Collections.singleton(Object.class.getName());
      this.declaredCache.keySet().retainAll(classNames);
      this.resRepCache.keySet().retainAll(classNames);
   }

   private static final class NO_METHOD_MARKER {
      private void noMethod() {
      }
   }
}
