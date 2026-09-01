package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class ColorConverter implements Converter {
   public boolean canConvert(Class type) {
      return type != null && type.getName().equals("java.awt.Color");
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Color color = (Color)source;
      this.write("red", color.getRed(), writer);
      this.write("green", color.getGreen(), writer);
      this.write("blue", color.getBlue(), writer);
      this.write("alpha", color.getAlpha(), writer);
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Map elements = new HashMap();

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         elements.put(reader.getNodeName(), Integer.valueOf(reader.getValue()));
         reader.moveUp();
      }

      return new Color((Integer)elements.get("red"), (Integer)elements.get("green"), (Integer)elements.get("blue"), (Integer)elements.get("alpha"));
   }

   private void write(String fieldName, int value, HierarchicalStreamWriter writer) {
      ExtendedHierarchicalStreamWriterHelper.startNode(writer, fieldName, int.class);
      writer.setValue(String.valueOf(value));
      writer.endNode();
   }
}
