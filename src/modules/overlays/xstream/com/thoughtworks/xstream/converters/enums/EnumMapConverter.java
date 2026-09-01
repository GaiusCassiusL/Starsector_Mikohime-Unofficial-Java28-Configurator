package com.thoughtworks.xstream.converters.enums;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.core.util.Fields;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;
import java.util.EnumMap;

public class EnumMapConverter extends MapConverter {
   public EnumMapConverter(Mapper mapper) {
      super(mapper);
   }

   @Override
   public boolean canConvert(Class type) {
      return type == EnumMap.class && EnumMapConverter.Reflections.typeField != null;
   }

   @Override
   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Class type = (Class)Fields.read(EnumMapConverter.Reflections.typeField, source);
      String attributeName = this.mapper().aliasForSystemAttribute("enum-type");
      if (attributeName != null) {
         writer.addAttribute(attributeName, this.mapper().serializedClass(type));
      }

      super.marshal(source, writer, context);
   }

   @Override
   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      String attributeName = this.mapper().aliasForSystemAttribute("enum-type");
      if (attributeName == null) {
         throw new ConversionException("No EnumType specified for EnumMap");
      }

      Class type = this.mapper().realClass(reader.getAttribute(attributeName));
      EnumMap map = new EnumMap(type);
      this.populateMap(reader, context, map);
      return map;
   }

   private static class Reflections {
      private static final Field typeField = Fields.locate(EnumMap.class, Class.class, false);
   }
}
