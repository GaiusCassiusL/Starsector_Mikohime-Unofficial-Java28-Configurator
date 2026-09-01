package com.thoughtworks.xstream.converters.basic;

public class FloatConverter extends AbstractSingleValueConverter {
   public boolean canConvert(Class type) {
      return type == float.class || type == Float.class;
   }

   public Object fromString(String str) {
      return Float.valueOf(str);
   }
}
