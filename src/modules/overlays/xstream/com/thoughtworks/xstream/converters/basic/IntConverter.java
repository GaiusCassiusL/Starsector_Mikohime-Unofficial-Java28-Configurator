package com.thoughtworks.xstream.converters.basic;

public class IntConverter extends AbstractSingleValueConverter {
   public boolean canConvert(Class type) {
      return type == int.class || type == Integer.class;
   }

   public Object fromString(String str) {
      long value = Long.decode(str);
      if (value >= -2147483648L && value <= 4294967295L) {
         return new Integer((int)value);
      } else {
         throw new NumberFormatException("For input string: \"" + str + '"');
      }
   }
}
