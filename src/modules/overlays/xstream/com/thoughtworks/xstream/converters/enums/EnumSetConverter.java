package com.thoughtworks.xstream.converters.enums;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.Fields;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;
import java.util.EnumSet;

public class EnumSetConverter implements Converter {
   private final Mapper mapper;

   public EnumSetConverter(Mapper mapper) {
      this.mapper = mapper;
   }

   @Override
   public boolean canConvert(Class type) {
      return type != null && EnumSet.class.isAssignableFrom(type) && EnumSetConverter.Reflections.typeField != null;
   }

   @Override
   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      EnumSet set = (EnumSet)source;
      Class enumTypeForSet = (Class)Fields.read(EnumSetConverter.Reflections.typeField, set);
      String attributeName = this.mapper.aliasForSystemAttribute("enum-type");
      if (attributeName != null) {
         writer.addAttribute(attributeName, this.mapper.serializedClass(enumTypeForSet));
      }

      writer.setValue(this.joinEnumValues(set));
   }

   private String joinEnumValues(EnumSet set) {
      boolean seenFirst = false;
      StringBuffer result = new StringBuffer();

      for (Enum value : set) {
         if (seenFirst) {
            result.append(',');
         } else {
            seenFirst = true;
         }

         result.append(value.name());
      }

      return result.toString();
   }

   @Override
   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      String attributeName = this.mapper.aliasForSystemAttribute("enum-type");
      if (attributeName == null) {
         throw new ConversionException("No EnumType specified for EnumSet");
      }

      Class enumTypeForSet = this.mapper.realClass(reader.getAttribute(attributeName));
      EnumSet set = EnumSet.noneOf(enumTypeForSet);
      String[] enumValues = reader.getValue().split(",");

      for (int i = 0; i < enumValues.length; i++) {
         String enumValue = enumValues[i];
         if (enumValue.length() > 0) {
            set.add(Enum.valueOf(enumTypeForSet, enumValue));
         }
      }

      return set;
   }

   private static class Reflections {
      private static final Field typeField = Fields.locate(EnumSet.class, Class.class, false);
   }
}
