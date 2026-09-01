package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Collections;

public class SingletonMapConverter extends MapConverter {
   private static final Class MAP = Collections.singletonMap(Boolean.TRUE, null).getClass();

   public SingletonMapConverter(Mapper mapper) {
      super(mapper);
   }

   public boolean canConvert(Class type) {
      return MAP == type;
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      reader.moveDown();
      Object key = this.readCompleteItem(reader, context, null);
      Object value = this.readCompleteItem(reader, context, null);
      reader.moveUp();
      return Collections.singletonMap(key, value);
   }
}
