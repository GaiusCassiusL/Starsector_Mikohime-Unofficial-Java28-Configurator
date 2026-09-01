package com.thoughtworks.xstream.core.util;

import java.util.Set;
import java.util.Map.Entry;

class WeakCache$4 implements WeakCache.Visitor {
   WeakCache$4(WeakCache this$0, Set var2) {
      this.this$0 = this$0;
      this.val$set = var2;
   }

   public Object visit(Object element) {
      Entry entry = (Entry)element;
      this.val$set.add(new WeakCache$4$1(this, entry));
      return null;
   }
}
