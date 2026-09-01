package com.thoughtworks.xstream.converters.enums;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

public class EnumSingleValueConverter extends AbstractSingleValueConverter {
   private final Class<? extends Enum> enumType;

   public EnumSingleValueConverter(Class<? extends Enum> type) {
      if (!Enum.class.isAssignableFrom(type) && type != Enum.class) {
         throw new IllegalArgumentException("Converter can only handle defined enums");
      }

      this.enumType = type;
   }

   @Override
   public boolean canConvert(Class type) {
      return type != null && this.enumType.isAssignableFrom(type);
   }

   @Override
   public String toString(Object obj) {
      return Enum.class.cast(obj).name();
   }

   @Override
   public Object fromString(String str) {
      return Enum.valueOf(this.enumType, str);
   }
}
