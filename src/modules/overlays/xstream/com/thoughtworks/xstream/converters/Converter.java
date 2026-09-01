package com.thoughtworks.xstream.converters;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public interface Converter extends ConverterMatcher {
   void marshal(Object var1, HierarchicalStreamWriter var2, MarshallingContext var3);

   Object unmarshal(HierarchicalStreamReader var1, UnmarshallingContext var2);
}
