package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicBooleanConverter extends BooleanConverter implements Converter {
   @Override
   public boolean canConvert(Class type) {
      return type != null && type == AtomicBoolean.class;
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
      AtomicBoolean atomicBoolean = new AtomicBoolean("1".equals(reader.getValue()));
      reader.moveUp();
      return atomicBoolean;
   }

   @Override
   public String toString(Object obj) {
      return super.toString(((AtomicBoolean)obj).get() ? Boolean.TRUE : Boolean.FALSE);
   }

   @Override
   public Object fromString(String str) {
      return new AtomicBoolean((Boolean)super.fromString(str));
   }
}
