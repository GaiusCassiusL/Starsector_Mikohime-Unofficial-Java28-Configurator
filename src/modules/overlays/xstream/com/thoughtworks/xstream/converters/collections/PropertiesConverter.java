package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.Fields;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;

public class PropertiesConverter implements Converter {
   private final boolean sort;

   public PropertiesConverter() {
      this(false);
   }

   public PropertiesConverter(boolean sort) {
      this.sort = sort;
   }

   public boolean canConvert(Class type) {
      return Properties.class == type;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Properties properties = (Properties)source;
      Map map = this.sort ? new TreeMap<>(properties) : properties;

      for (Entry entry : map.entrySet()) {
         writer.startNode("property");
         writer.addAttribute("name", entry.getKey().toString());
         writer.addAttribute("value", entry.getValue().toString());
         writer.endNode();
      }

      if (PropertiesConverter.Reflections.defaultsField != null) {
         Properties defaults = (Properties)Fields.read(PropertiesConverter.Reflections.defaultsField, properties);
         if (defaults != null) {
            writer.startNode("defaults");
            this.marshal(defaults, writer, context);
            writer.endNode();
         }
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Properties properties = new Properties();
      Properties defaults = null;

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         if (reader.getNodeName().equals("defaults")) {
            defaults = (Properties)this.unmarshal(reader, context);
         } else {
            String name = reader.getAttribute("name");
            String value = reader.getAttribute("value");
            properties.setProperty(name, value);
         }

         reader.moveUp();
      }

      if (defaults == null) {
         return properties;
      }

      Properties propertiesWithDefaults = new Properties(defaults);
      propertiesWithDefaults.putAll(properties);
      return propertiesWithDefaults;
   }

   private static class Reflections {
      private static final Field defaultsField = Fields.locate(Properties.class, Properties.class, false);
   }
}
