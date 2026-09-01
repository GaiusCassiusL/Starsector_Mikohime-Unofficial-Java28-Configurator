package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import javax.activation.ActivationDataFlavor;

public class ActivationDataFlavorConverter implements Converter {
   public boolean canConvert(Class type) {
      return type == ActivationDataFlavor.class;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      ActivationDataFlavor dataFlavor = (ActivationDataFlavor)source;
      String mimeType = dataFlavor.getMimeType();
      if (mimeType != null) {
         writer.startNode("mimeType");
         writer.setValue(mimeType);
         writer.endNode();
      }

      String name = dataFlavor.getHumanPresentableName();
      if (name != null) {
         writer.startNode("humanRepresentableName");
         writer.setValue(name);
         writer.endNode();
      }

      Class representationClass = dataFlavor.getRepresentationClass();
      if (representationClass != null) {
         writer.startNode("representationClass");
         context.convertAnother(representationClass);
         writer.endNode();
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      String mimeType = null;
      String name = null;
      Class type = null;

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         String elementName = reader.getNodeName();
         if (elementName.equals("mimeType")) {
            mimeType = reader.getValue();
         } else if (elementName.equals("humanRepresentableName")) {
            name = reader.getValue();
         } else {
            if (!elementName.equals("representationClass")) {
               ConversionException exception = new ConversionException("Unknown child element");
               exception.add("element", reader.getNodeName());
               throw exception;
            }

            type = (Class)context.convertAnother(null, Class.class);
         }

         reader.moveUp();
      }

      ActivationDataFlavor dataFlavor = null;

      try {
         if (type == null) {
            dataFlavor = new ActivationDataFlavor(mimeType, name);
         } else if (mimeType == null) {
            dataFlavor = new ActivationDataFlavor(type, name);
         } else {
            dataFlavor = new ActivationDataFlavor(type, mimeType, name);
         }

         return dataFlavor;
      } catch (IllegalArgumentException ex) {
         throw new ConversionException(ex);
      } catch (NullPointerException ex) {
         throw new ConversionException(ex);
      }
   }
}
