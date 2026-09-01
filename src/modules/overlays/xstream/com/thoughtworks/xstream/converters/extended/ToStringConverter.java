package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ToStringConverter extends AbstractSingleValueConverter {
   private static final Class[] STRING_PARAMETER = new Class[]{String.class};
   private final Class clazz;
   private final Constructor ctor;

   public ToStringConverter(Class clazz) throws NoSuchMethodException {
      this.clazz = clazz;
      this.ctor = clazz.getConstructor(STRING_PARAMETER);
   }

   public boolean canConvert(Class type) {
      return type == this.clazz;
   }

   public String toString(Object obj) {
      return obj == null ? null : obj.toString();
   }

   public Object fromString(String str) {
      try {
         return this.ctor.newInstance(str);
      } catch (InstantiationException e) {
         throw new ConversionException("Unable to instantiate single String param constructor", e);
      } catch (IllegalAccessException e) {
         throw new ObjectAccessException("Unable to access single String param constructor", e);
      } catch (InvocationTargetException e) {
         throw new ConversionException("Unable to target single String param constructor", e.getTargetException());
      }
   }
}
