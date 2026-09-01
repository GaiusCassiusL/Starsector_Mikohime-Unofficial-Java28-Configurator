package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.DoubleConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.OptionalDouble;

public class OptionalDoubleConverter extends DoubleConverter implements Converter {
   @Override
   public boolean canConvert(Class type) {
      return type != null && type == OptionalDouble.class;
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
      double value = (Double)context.convertAnother(context, Double.class);
      reader.moveUp();
      return isPresent ? OptionalDouble.of(value) : OptionalDouble.empty();
   }

   @Override
   public String toString(Object obj) {
      OptionalDouble optional = (OptionalDouble)obj;
      return optional.isPresent() ? super.toString(optional.getAsDouble()) : "";
   }

   @Override
   public Object fromString(String str) {
      return str != null && str.length() != 0 ? OptionalDouble.of((Double)super.fromString(str)) : OptionalDouble.empty();
   }
}
