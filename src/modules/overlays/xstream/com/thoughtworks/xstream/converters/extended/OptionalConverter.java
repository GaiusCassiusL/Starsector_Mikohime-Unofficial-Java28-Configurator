package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Optional;

public class OptionalConverter implements Converter {
   private Mapper mapper;

   public OptionalConverter(Mapper mapper) {
      this.mapper = mapper;
   }

   @Override
   public boolean canConvert(Class type) {
      return type != null && type == Optional.class;
   }

   @Override
   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Optional optional = (Optional)source;
      if (optional.isPresent()) {
         writer.startNode(this.mapper.serializedMember(Optional.class, "value"));
         Object object = optional.get();
         String name = this.mapper.serializedClass(object != null ? object.getClass() : null);
         writer.addAttribute(this.mapper.aliasForSystemAttribute("class"), name);
         context.convertAnother(optional.get());
         writer.endNode();
      }
   }

   @Override
   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      if (reader.hasMoreChildren()) {
         reader.moveDown();
         Class type = HierarchicalStreams.readClassType(reader, this.mapper);
         Object value = context.convertAnother(context, type);
         reader.moveUp();
         return Optional.of(value);
      } else {
         return Optional.empty();
      }
   }
}
