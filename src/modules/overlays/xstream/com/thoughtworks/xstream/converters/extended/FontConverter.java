package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.plaf.FontUIResource;

public class FontConverter implements Converter {
   private final SingleValueConverter textAttributeConverter;
   private final Mapper mapper;

   /** @deprecated */
   public FontConverter() {
      this(null);
   }

   public FontConverter(Mapper mapper) {
      this.mapper = mapper;
      if (mapper == null) {
         this.textAttributeConverter = null;
      } else {
         this.textAttributeConverter = new TextAttributeConverter();
      }
   }

   public boolean canConvert(Class type) {
      return type != null && (type.getName().equals("java.awt.Font") || type.getName().equals("javax.swing.plaf.FontUIResource"));
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Font font = (Font)source;
      Map attributes = font.getAttributes();
      if (this.mapper != null) {
         String classAlias = this.mapper.aliasForSystemAttribute("class");

         for (Entry entry : attributes.entrySet()) {
            String name = this.textAttributeConverter.toString(entry.getKey());
            Object value = entry.getValue();
            Class type = value != null ? value.getClass() : Mapper.Null.class;
            ExtendedHierarchicalStreamWriterHelper.startNode(writer, name, type);
            writer.addAttribute(classAlias, this.mapper.serializedClass(type));
            if (value != null) {
               context.convertAnother(value);
            }

            writer.endNode();
         }
      } else {
         writer.startNode("attributes");
         context.convertAnother(attributes);
         writer.endNode();
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Map attributes;
      if (reader.hasMoreChildren()) {
         reader.moveDown();
         if (!reader.getNodeName().equals("attributes")) {
            String classAlias = this.mapper.aliasForSystemAttribute("class");
            attributes = new HashMap();

            do {
               if (!attributes.isEmpty()) {
                  reader.moveDown();
               }

               Class type = this.mapper.realClass(reader.getAttribute(classAlias));
               TextAttribute attribute = (TextAttribute)this.textAttributeConverter.fromString(reader.getNodeName());
               Object value = type == Mapper.Null.class ? null : context.convertAnother(null, type);
               attributes.put(attribute, value);
               reader.moveUp();
            } while (reader.hasMoreChildren());
         } else {
            attributes = (Map)context.convertAnother(null, Map.class);
            reader.moveUp();
         }
      } else {
         attributes = Collections.EMPTY_MAP;
      }

      if (!JVM.isVersion(6)) {
         Iterator iter = attributes.values().iterator();

         while (iter.hasNext()) {
            if (iter.next() == null) {
               iter.remove();
            }
         }
      }

      Font font = Font.getFont(attributes);
      return context.getRequiredType() == FontUIResource.class ? new FontUIResource(font) : font;
   }
}
