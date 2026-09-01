package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.regex.Pattern;

public class RegexPatternConverter implements Converter {
   public RegexPatternConverter() {
   }

   /** @deprecated */
   public RegexPatternConverter(Converter defaultConverter) {
   }

   public boolean canConvert(Class type) {
      return type == Pattern.class;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Pattern pattern = (Pattern)source;
      writer.startNode("pattern");
      writer.setValue(pattern.pattern());
      writer.endNode();
      writer.startNode("flags");
      writer.setValue(String.valueOf(pattern.flags()));
      writer.endNode();
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      reader.moveDown();
      String pattern = reader.getValue();
      reader.moveUp();
      reader.moveDown();
      int flags = Integer.parseInt(reader.getValue());
      reader.moveUp();
      return Pattern.compile(pattern, flags);
   }
}
