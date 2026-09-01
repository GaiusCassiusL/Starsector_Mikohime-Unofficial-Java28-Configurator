package com.thoughtworks.xstream.io;

import com.thoughtworks.xstream.core.util.Cloneables;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.naming.NoNameCoder;

public abstract class AbstractWriter implements ExtendedHierarchicalStreamWriter {
   private NameCoder nameCoder;

   protected AbstractWriter() {
      this(new NoNameCoder());
   }

   protected AbstractWriter(NameCoder nameCoder) {
      this.nameCoder = (NameCoder)Cloneables.cloneIfPossible(nameCoder);
   }

   public void startNode(String name, Class clazz) {
      this.startNode(name);
   }

   public HierarchicalStreamWriter underlyingWriter() {
      return this;
   }

   public String encodeNode(String name) {
      return this.nameCoder.encodeNode(name);
   }

   public String encodeAttribute(String name) {
      return this.nameCoder.encodeAttribute(name);
   }
}
