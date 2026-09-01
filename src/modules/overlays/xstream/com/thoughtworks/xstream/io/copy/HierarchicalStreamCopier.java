package com.thoughtworks.xstream.io.copy;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class HierarchicalStreamCopier {
   public void copy(HierarchicalStreamReader source, HierarchicalStreamWriter destination) {
      destination.startNode(source.getNodeName());
      int attributeCount = source.getAttributeCount();

      for (int i = 0; i < attributeCount; i++) {
         destination.addAttribute(source.getAttributeName(i), source.getAttribute(i));
      }

      String value = source.getValue();
      if (value != null && value.length() > 0) {
         destination.setValue(value);
      }

      while (source.hasMoreChildren()) {
         source.moveDown();
         this.copy(source, destination);
         source.moveUp();
      }

      destination.endNode();
   }
}
