package com.thoughtworks.xstream.converters.collections;

import java.util.AbstractList;
import java.util.Map;

class TreeSetConverter$1$1 extends AbstractList {
   TreeSetConverter$1$1(TreeSetConverter$1 this$1, Map var2) {
      this.this$1 = this$1;
      this.val$target = var2;
   }

   public boolean add(Object object) {
      return this.val$target.put(object, TreeSetConverter.Reflections.access$100() != null ? TreeSetConverter.Reflections.access$100() : object) != null;
   }

   public Object get(int location) {
      return null;
   }

   public int size() {
      return this.val$target.size();
   }
}
