package com.thoughtworks.xstream.converters.reflection;

import java.lang.reflect.Field;

class FieldUtil15 implements FieldDictionary.FieldUtil {
   @Override
   public boolean isSynthetic(Field field) {
      return field.isSynthetic();
   }
}
