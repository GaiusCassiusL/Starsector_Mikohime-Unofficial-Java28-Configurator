package com.thoughtworks.xstream.io;

import java.util.Iterator;

/** @deprecated */
public class AttributeNameIterator implements Iterator {
   private int current;
   private final int count;
   private final HierarchicalStreamReader reader;

   public AttributeNameIterator(HierarchicalStreamReader reader) {
      this.reader = reader;
      this.count = reader.getAttributeCount();
   }

   public boolean hasNext() {
      return this.current < this.count;
   }

   public Object next() {
      return this.reader.getAttributeName(this.current++);
   }

   public void remove() {
      throw new UnsupportedOperationException();
   }
}
