package com.thoughtworks.xstream;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public interface MarshallingStrategy {
   Object unmarshal(Object var1, HierarchicalStreamReader var2, DataHolder var3, ConverterLookup var4, Mapper var5);

   void marshal(HierarchicalStreamWriter var1, Object var2, ConverterLookup var3, Mapper var4, DataHolder var5);
}
