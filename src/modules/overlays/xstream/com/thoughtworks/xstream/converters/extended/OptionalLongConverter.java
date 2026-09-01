package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.LongConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.OptionalLong;

public class OptionalLongConverter extends LongConverter implements Converter {
   @Override
   public boolean canConvert(Class type) {
      return type != null && type == OptionalLong.class;
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
      long value = (Long)context.convertAnother(context, Long.class);
      reader.moveUp();
      return isPresent ? OptionalLong.of(value) : OptionalLong.empty();
   }

   @Override
   public String toString(Object obj) {
      OptionalLong optional = (OptionalLong)obj;
      return optional.isPresent() ? super.toString(optional.getAsLong()) : "";
   }

   @Override
   public Object fromString(String str) {
      return str != null && str.length() != 0 ? OptionalLong.of((Long)super.fromString(str)) : OptionalLong.empty();
   }
}
