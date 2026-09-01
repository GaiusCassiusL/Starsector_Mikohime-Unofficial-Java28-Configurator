package com.thoughtworks.xstream.io;

import com.thoughtworks.xstream.core.util.Cloneables;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.naming.NoNameCoder;

public abstract class AbstractReader implements ExtendedHierarchicalStreamReader {
   private NameCoder nameCoder;

   protected AbstractReader() {
      this(new NoNameCoder());
   }

   protected AbstractReader(NameCoder nameCoder) {
      this.nameCoder = (NameCoder)Cloneables.cloneIfPossible(nameCoder);
   }

   public HierarchicalStreamReader underlyingReader() {
      return this;
   }

   public String decodeNode(String name) {
      return this.nameCoder.decodeNode(name);
   }

   public String decodeAttribute(String name) {
      return this.nameCoder.decodeAttribute(name);
   }

   protected String encodeNode(String name) {
      return this.nameCoder.encodeNode(name);
   }

   protected String encodeAttribute(String name) {
      return this.nameCoder.encodeAttribute(name);
   }

   public String peekNextChild() {
      throw new UnsupportedOperationException("peekNextChild");
   }
}
