package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.IntConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerConverter extends IntConverter implements Converter {
   @Override
   public boolean canConvert(Class type) {
      return type != null && type == AtomicInteger.class;
   }

   @Override
   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      writer.setValue(this.toString(source));
   }

   @Override
   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      String data = reader.getValue();
      if (!reader.hasMoreChildren()) {
         return this.fromString(data);
      }

      reader.moveDown();
      Integer integer = (Integer)super.fromString(reader.getValue());
      reader.moveUp();
      return new AtomicInteger(integer);
   }

   @Override
   public String toString(Object obj) {
      return super.toString(new Integer(((AtomicInteger)obj).get()));
   }

   @Override
   public Object fromString(String str) {
      return new AtomicInteger((Integer)super.fromString(str));
   }
}
