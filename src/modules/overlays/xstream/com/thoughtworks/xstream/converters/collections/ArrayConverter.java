package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayConverter extends AbstractCollectionConverter {
   public ArrayConverter(Mapper mapper) {
      super(mapper);
   }

   public boolean canConvert(Class type) {
      return type != null && type.isArray();
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      int length = Array.getLength(source);

      for (int i = 0; i < length; i++) {
         Object item = Array.get(source, i);
         this.writeCompleteItem(item, context, writer);
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      List items = new ArrayList();

      while (reader.hasMoreChildren()) {
         Object item = this.readCompleteItem(reader, context, null);
         items.add(item);
      }

      Object array = Array.newInstance(context.getRequiredType().getComponentType(), items.size());
      int i = 0;
      Iterator iterator = items.iterator();

      while (iterator.hasNext()) {
         Array.set(array, i++, iterator.next());
      }

      return array;
   }
}
