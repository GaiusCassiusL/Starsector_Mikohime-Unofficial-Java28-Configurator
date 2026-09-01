package com.thoughtworks.xstream.converters.basic;

public class StringBuilderConverter extends AbstractSingleValueConverter {
   public Object fromString(String str) {
      return new StringBuilder(str);
   }

   public boolean canConvert(Class type) {
      return type == StringBuilder.class;
   }
}
