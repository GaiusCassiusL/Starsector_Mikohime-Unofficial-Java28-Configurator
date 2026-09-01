package com.thoughtworks.xstream.io;

/** @deprecated */
public class ExtendedHierarchicalStreamWriterHelper {
   /** @deprecated */
   public static void startNode(HierarchicalStreamWriter writer, String name, Class clazz) {
      if (writer instanceof ExtendedHierarchicalStreamWriter) {
         ((ExtendedHierarchicalStreamWriter)writer).startNode(name, clazz);
      } else {
         writer.startNode(name);
      }
   }
}
