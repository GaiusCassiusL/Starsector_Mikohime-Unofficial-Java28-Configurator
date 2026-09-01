package com.thoughtworks.xstream.io.json;

import java.io.Writer;

/** @deprecated */
public class JsonHierarchicalStreamWriter extends JsonWriter {
   /** @deprecated */
   public JsonHierarchicalStreamWriter(Writer writer, char[] lineIndenter, String newLine) {
      super(writer, lineIndenter, newLine);
   }

   /** @deprecated */
   public JsonHierarchicalStreamWriter(Writer writer, char[] lineIndenter) {
      this(writer, lineIndenter, "\n");
   }

   /** @deprecated */
   public JsonHierarchicalStreamWriter(Writer writer, String lineIndenter, String newLine) {
      this(writer, lineIndenter.toCharArray(), newLine);
   }

   /** @deprecated */
   public JsonHierarchicalStreamWriter(Writer writer, String lineIndenter) {
      this(writer, lineIndenter.toCharArray());
   }

   /** @deprecated */
   public JsonHierarchicalStreamWriter(Writer writer) {
      this(writer, new char[]{' ', ' '});
   }
}
