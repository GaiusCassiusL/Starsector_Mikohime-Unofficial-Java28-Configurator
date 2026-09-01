package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceConverter implements Converter {
   private Mapper mapper;

   public AtomicReferenceConverter(Mapper mapper) {
      this.mapper = mapper;
   }

   @Override
   public boolean canConvert(Class type) {
      return type != null && type == AtomicReference.class;
   }

   @Override
   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      AtomicReference ref = (AtomicReference)source;
      Object object = ref.get();
      if (object != null) {
         writer.startNode(this.mapper.serializedMember(AtomicReference.class, "value"));
         String name = this.mapper.serializedClass(object.getClass());
         writer.addAttribute(this.mapper.aliasForSystemAttribute("class"), name);
         context.convertAnother(object);
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
         return new AtomicReference<>(value);
      } else {
         return new AtomicReference();
      }
   }
}
