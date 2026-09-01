package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.ErrorWritingException;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

public class SunLimitedUnsafeReflectionProvider extends PureJavaReflectionProvider {
   protected static final Unsafe unsafe;
   protected static final Exception exception;

   public SunLimitedUnsafeReflectionProvider() {
   }

   public SunLimitedUnsafeReflectionProvider(FieldDictionary fieldDictionary) {
      super(fieldDictionary);
   }

   public Object newInstance(Class type) {
      if (exception != null) {
         ObjectAccessException ex = new ObjectAccessException("Cannot construct type", exception);
         ex.add("construction-type", type.getName());
         throw ex;
      }

      ErrorWritingException ex = null;
      if (type != void.class && type != Void.class) {
         try {
            return unsafe.allocateInstance(type);
         } catch (SecurityException e) {
            ex = new ObjectAccessException("Cannot construct type", e);
         } catch (InstantiationException e) {
            ex = new ConversionException("Cannot construct type", e);
         } catch (IllegalArgumentException e) {
            ex = new ObjectAccessException("Cannot construct type", e);
         }
      } else {
         ex = new ConversionException("Security alert: Marshalling rejected");
      }

      ex.add("construction-type", type.getName());
      throw ex;
   }

   protected void validateFieldAccess(Field field) {
   }

   private Object readResolve() {
      this.init();
      return this;
   }

   static {
      Unsafe u = null;
      Exception ex = null;

      try {
         Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
         unsafeField.setAccessible(true);
         u = (Unsafe)unsafeField.get(null);
      } catch (SecurityException e) {
         ex = e;
      } catch (NoSuchFieldException e) {
         ex = e;
      } catch (IllegalArgumentException e) {
         ex = e;
      } catch (IllegalAccessException e) {
         ex = e;
      }

      exception = ex;
      unsafe = u;
   }
}
