package com.thoughtworks.xstream.converters.basic;

public class DoubleConverter extends AbstractSingleValueConverter {
   public boolean canConvert(Class type) {
      return type == double.class || type == Double.class;
   }

   public Object fromString(String str) {
      return Double.valueOf(str);
   }
}
