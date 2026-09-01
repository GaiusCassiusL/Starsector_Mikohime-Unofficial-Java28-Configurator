package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.core.JVM;
import java.lang.reflect.InvocationTargetException;
import java.util.GregorianCalendar;

public class ISO8601GregorianCalendarConverter extends AbstractSingleValueConverter {
   private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
   private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
   private final SingleValueConverter converter;

   public ISO8601GregorianCalendarConverter() {
      SingleValueConverter svConverter = null;
      Class type = JVM.loadClassForName(
         JVM.isVersion(8) ? "com.thoughtworks.xstream.core.util.ISO8601JavaTimeConverter" : "com.thoughtworks.xstream.core.util.ISO8601JodaTimeConverter"
      );

      try {
         svConverter = (SingleValueConverter)type.getDeclaredConstructor(EMPTY_CLASS_ARRAY).newInstance(EMPTY_OBJECT_ARRAY);
      } catch (InstantiationException var4) {
      } catch (IllegalAccessException var5) {
      } catch (InvocationTargetException var6) {
      } catch (NoSuchMethodException var7) {
      }

      this.converter = svConverter;
   }

   public boolean canConvert(Class type) {
      return this.converter != null && type == GregorianCalendar.class;
   }

   public Object fromString(String str) {
      return this.converter.fromString(str);
   }

   public String toString(Object obj) {
      return this.converter.toString(obj);
   }
}
