package com.thoughtworks.xstream.converters.basic;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class CharConverter implements Converter, SingleValueConverter {
   public boolean canConvert(Class type) {
      return type == char.class || type == Character.class;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      writer.setValue(this.toString(source));
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      String nullAttribute = reader.getAttribute("null");
      return nullAttribute != null && nullAttribute.equals("true") ? new Character('\u0000') : this.fromString(reader.getValue());
   }

   public Object fromString(String str) {
      return str.length() == 0 ? new Character('\u0000') : new Character(str.charAt(0));
   }

   public String toString(Object obj) {
      char ch = (Character)obj;
      return ch == 0 ? "" : obj.toString();
   }
}
