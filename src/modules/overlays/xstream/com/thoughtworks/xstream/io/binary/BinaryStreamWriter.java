package com.thoughtworks.xstream.io.binary;

import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class BinaryStreamWriter implements ExtendedHierarchicalStreamWriter {
   private final BinaryStreamWriter.IdRegistry idRegistry = new BinaryStreamWriter.IdRegistry(null);
   private final DataOutputStream out;
   private final Token.Formatter tokenFormatter = new Token.Formatter();

   public BinaryStreamWriter(OutputStream outputStream) {
      this.out = new DataOutputStream(outputStream);
   }

   public void startNode(String name) {
      this.write(new Token.StartNode(this.idRegistry.getId(name)));
   }

   public void startNode(String name, Class clazz) {
      this.startNode(name);
   }

   public void addAttribute(String name, String value) {
      this.write(new Token.Attribute(this.idRegistry.getId(name), value));
   }

   public void setValue(String text) {
      this.write(new Token.Value(text));
   }

   public void endNode() {
      this.write(new Token.EndNode());
   }

   public void flush() {
      try {
         this.out.flush();
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public void close() {
      try {
         this.out.close();
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamWriter underlyingWriter() {
      return this;
   }

   private void write(Token token) {
      try {
         this.tokenFormatter.write(this.out, token);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   private class IdRegistry {
      private long nextId = 0L;
      private Map ids = new HashMap();

      private IdRegistry() {
      }

      public long getId(String value) {
         Long id = (Long)this.ids.get(value);
         if (id == null) {
            id = new Long(++this.nextId);
            this.ids.put(value, id);
            BinaryStreamWriter.this.write(new Token.MapIdToValue(id, value));
         }

         return id;
      }
   }
}
