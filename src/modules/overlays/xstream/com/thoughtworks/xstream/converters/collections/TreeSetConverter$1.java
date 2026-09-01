package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Map;

class TreeSetConverter$1 extends TreeMapConverter {
   TreeSetConverter$1(TreeSetConverter this$0, Mapper mapper) {
      super(mapper);
      this.this$0 = this$0;
   }

   protected void populateMap(HierarchicalStreamReader reader, UnmarshallingContext context, Map map, Map target) {
      this.this$0.populateCollection(reader, context, new TreeSetConverter$1$1(this, target));
   }

   protected void putCurrentEntryIntoMap(HierarchicalStreamReader reader, UnmarshallingContext context, Map map, Map target) {
      Object key = this.readItem(reader, context, map);
      target.put(key, key);
   }
}
