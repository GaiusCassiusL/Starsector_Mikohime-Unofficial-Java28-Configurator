package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ErrorWritingException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public abstract class AbstractCollectionConverter implements Converter {
   private final Mapper mapper;

   public abstract boolean canConvert(Class var1);

   public AbstractCollectionConverter(Mapper mapper) {
      this.mapper = mapper;
   }

   protected Mapper mapper() {
      return this.mapper;
   }

   public abstract void marshal(Object var1, HierarchicalStreamWriter var2, MarshallingContext var3);

   public abstract Object unmarshal(HierarchicalStreamReader var1, UnmarshallingContext var2);

   /** @deprecated */
   protected void writeItem(Object item, MarshallingContext context, HierarchicalStreamWriter writer) {
      if (item == null) {
         this.writeNullItem(context, writer);
      } else {
         String name = this.mapper().serializedClass(item.getClass());
         ExtendedHierarchicalStreamWriterHelper.startNode(writer, name, item.getClass());
         this.writeBareItem(item, context, writer);
         writer.endNode();
      }
   }

   protected void writeCompleteItem(Object item, MarshallingContext context, HierarchicalStreamWriter writer) {
      this.writeItem(item, context, writer);
   }

   protected void writeBareItem(Object item, MarshallingContext context, HierarchicalStreamWriter writer) {
      context.convertAnother(item);
   }

   protected void writeNullItem(MarshallingContext context, HierarchicalStreamWriter writer) {
      String name = this.mapper().serializedClass(null);
      ExtendedHierarchicalStreamWriterHelper.startNode(writer, name, Mapper.Null.class);
      writer.endNode();
   }

   /** @deprecated */
   protected Object readItem(HierarchicalStreamReader reader, UnmarshallingContext context, Object current) {
      return this.readBareItem(reader, context, current);
   }

   protected Object readBareItem(HierarchicalStreamReader reader, UnmarshallingContext context, Object current) {
      Class type = HierarchicalStreams.readClassType(reader, this.mapper());
      return context.convertAnother(current, type);
   }

   protected Object readCompleteItem(HierarchicalStreamReader reader, UnmarshallingContext context, Object current) {
      reader.moveDown();
      Object result = this.readItem(reader, context, current);
      reader.moveUp();
      return result;
   }

   protected Object createCollection(Class type) {
      ErrorWritingException ex = null;
      Class defaultType = this.mapper().defaultImplementationOf(type);

      try {
         return defaultType.newInstance();
      } catch (InstantiationException e) {
         ex = new ConversionException("Cannot instantiate default collection", e);
      } catch (IllegalAccessException e) {
         ex = new ObjectAccessException("Cannot instantiate default collection", e);
      }

      ex.add("collection-type", type.getName());
      ex.add("default-type", defaultType.getName());
      throw ex;
   }
}
