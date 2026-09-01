package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class NamedCollectionConverter extends CollectionConverter {
   private final String name;
   private final Class type;

   public NamedCollectionConverter(Mapper mapper, String itemName, Class itemType) {
      this(null, mapper, itemName, itemType);
   }

   public NamedCollectionConverter(Class type, Mapper mapper, String itemName, Class itemType) {
      super(mapper, type);
      this.name = itemName;
      this.type = itemType;
   }

   protected void writeCompleteItem(Object item, MarshallingContext context, HierarchicalStreamWriter writer) {
      this.writeItem(item, context, writer);
   }

   /** @deprecated */
   protected void writeItem(Object item, MarshallingContext context, HierarchicalStreamWriter writer) {
      Class itemType = item == null ? Mapper.Null.class : item.getClass();
      ExtendedHierarchicalStreamWriterHelper.startNode(writer, this.name, itemType);
      if (!itemType.equals(this.type)) {
         String attributeName = this.mapper().aliasForSystemAttribute("class");
         if (attributeName != null) {
            writer.addAttribute(attributeName, this.mapper().serializedClass(itemType));
         }
      }

      if (item != null) {
         context.convertAnother(item);
      }

      writer.endNode();
   }

   protected Object readBareItem(HierarchicalStreamReader reader, UnmarshallingContext context, Object current) {
      String className = HierarchicalStreams.readClassAttribute(reader, this.mapper());
      Class itemType = className == null ? this.type : this.mapper().realClass(className);
      return Mapper.Null.class.equals(itemType) ? null : context.convertAnother(current, itemType);
   }
}
