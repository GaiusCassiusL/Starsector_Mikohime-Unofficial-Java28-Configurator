package com.thoughtworks.xstream.io.binary;

import java.util.Iterator;

class ReaderDepthState$1 implements Iterator {
   ReaderDepthState$1(ReaderDepthState this$0, Iterator var2) {
      this.this$0 = this$0;
      this.val$attributeIterator = var2;
   }

   public boolean hasNext() {
      return this.val$attributeIterator.hasNext();
   }

   public Object next() {
      ReaderDepthState.Attribute attribute = (ReaderDepthState.Attribute)this.val$attributeIterator.next();
      return attribute.name;
   }

   public void remove() {
      throw new UnsupportedOperationException();
   }
}
