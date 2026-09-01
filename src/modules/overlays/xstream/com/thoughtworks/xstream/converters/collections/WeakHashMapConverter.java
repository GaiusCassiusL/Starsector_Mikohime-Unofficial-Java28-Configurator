package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.WeakHashMap;

public class WeakHashMapConverter implements Converter {
   public boolean canConvert(Class type) {
      return WeakHashMap.class == type;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      return new WeakHashMap();
   }
}
