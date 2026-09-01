package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.IntConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.OptionalInt;

public class OptionalIntConverter extends IntConverter implements Converter {
   @Override
   public boolean canConvert(Class type) {
      return type != null && type == OptionalInt.class;
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
      boolean isPresent = (Boolean)context.convertAnother(context, Boolean.class);
      reader.moveUp();
      reader.moveDown();
      int value = (Integer)context.convertAnother(context, Integer.class);
      reader.moveUp();
      return isPresent ? OptionalInt.of(value) : OptionalInt.empty();
   }

   @Override
   public String toString(Object obj) {
      OptionalInt optional = (OptionalInt)obj;
      return optional.isPresent() ? super.toString(optional.getAsInt()) : "";
   }

   @Override
   public Object fromString(String str) {
      return str != null && str.length() != 0 ? OptionalInt.of((Integer)super.fromString(str)) : OptionalInt.empty();
   }
}
