package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Collections;

public class SingletonCollectionConverter extends CollectionConverter {
   private static final Class LIST = Collections.singletonList(Boolean.TRUE).getClass();
   private static final Class SET = Collections.singleton(Boolean.TRUE).getClass();

   public SingletonCollectionConverter(Mapper mapper) {
      super(mapper);
   }

   public boolean canConvert(Class type) {
      return LIST == type || SET == type;
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Object item = this.readCompleteItem(reader, context, null);
      return context.getRequiredType() == LIST ? Collections.singletonList(item) : Collections.singleton(item);
   }
}
