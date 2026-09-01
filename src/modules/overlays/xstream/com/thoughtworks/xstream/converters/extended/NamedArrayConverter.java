package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.core.util.Primitives;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class NamedArrayConverter implements Converter {
   private final Class arrayType;
   private final String itemName;
   private final Mapper mapper;

   public NamedArrayConverter(Class arrayType, Mapper mapper, String itemName) {
      if (!arrayType.isArray()) {
         throw new IllegalArgumentException(arrayType.getName() + " is not an array");
      }

      this.arrayType = arrayType;
      this.mapper = mapper;
      this.itemName = itemName;
   }

   public boolean canConvert(Class type) {
      return type == this.arrayType;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      int length = Array.getLength(source);

      for (int i = 0; i < length; i++) {
         Object item = Array.get(source, i);
         Class itemType = item == null
            ? Mapper.Null.class
            : (this.arrayType.getComponentType().isPrimitive() ? Primitives.unbox(item.getClass()) : item.getClass());
         ExtendedHierarchicalStreamWriterHelper.startNode(writer, this.itemName, itemType);
         if (!itemType.equals(this.arrayType.getComponentType())) {
            String attributeName = this.mapper.aliasForSystemAttribute("class");
            if (attributeName != null) {
               writer.addAttribute(attributeName, this.mapper.serializedClass(itemType));
            }
         }

         if (item != null) {
            context.convertAnother(item);
         }

         writer.endNode();
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      List list = new ArrayList();

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         String className = HierarchicalStreams.readClassAttribute(reader, this.mapper);
         Class itemType = className == null ? this.arrayType.getComponentType() : this.mapper.realClass(className);
         Object item;
         if (Mapper.Null.class.equals(itemType)) {
            item = null;
         } else {
            item = context.convertAnother(null, itemType);
         }

         list.add(item);
         reader.moveUp();
      }

      Object array = Array.newInstance(this.arrayType.getComponentType(), list.size());

      for (int i = 0; i < list.size(); i++) {
         Array.set(array, i, list.get(i));
      }

      return array;
   }
}
