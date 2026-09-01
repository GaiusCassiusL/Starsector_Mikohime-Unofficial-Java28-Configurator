package com.thoughtworks.xstream.core.util;

import java.util.Collection;

class WeakCache$3 implements WeakCache.Visitor {
   WeakCache$3(WeakCache this$0, Collection var2) {
      this.this$0 = this$0;
      this.val$collection = var2;
   }

   public Object visit(Object element) {
      this.val$collection.add(element);
      return null;
   }
}
