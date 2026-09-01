package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Map;

public class ValueRangeConverter implements Converter {
   private final Mapper mapper;

   public ValueRangeConverter(Mapper mapper) {
      this.mapper = mapper;
   }

   @Override
   public boolean canConvert(Class type) {
      return type == ValueRange.class;
   }

   @Override
   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      ValueRange valueRange = (ValueRange)source;
      this.write("maxLargest", valueRange.getMaximum(), writer);
      this.write("maxSmallest", valueRange.getSmallestMaximum(), writer);
      this.write("minLargest", valueRange.getLargestMinimum(), writer);
      this.write("minSmallest", valueRange.getMinimum(), writer);
   }

   @Override
   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      boolean oldFormat = "custom".equals(reader.getAttribute(this.mapper.aliasForSystemAttribute("serialization")));
      if (oldFormat) {
         reader.moveDown();
         reader.moveDown();
      }

      Map<String, Long> elements = new HashMap<>();

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         String name = reader.getNodeName();
         elements.put(oldFormat ? name : this.mapper.realMember(ValueRange.class, name), Long.valueOf(reader.getValue()));
         reader.moveUp();
      }

      if (oldFormat) {
         reader.moveUp();
         reader.moveUp();
      }

      return ValueRange.of(elements.get("minSmallest"), elements.get("minLargest"), elements.get("maxSmallest"), elements.get("maxLargest"));
   }

   private void write(String fieldName, long value, HierarchicalStreamWriter writer) {
      ExtendedHierarchicalStreamWriterHelper.startNode(writer, this.mapper.serializedMember(ValueRange.class, fieldName), long.class);
      writer.setValue(String.valueOf(value));
      writer.endNode();
   }
}
