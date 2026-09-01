package com.thoughtworks.xstream.core.util;

class WeakCache$1 implements WeakCache.Visitor {
   WeakCache$1(WeakCache this$0, Object var2) {
      this.this$0 = this$0;
      this.val$value = var2;
   }

   public Object visit(Object element) {
      return element.equals(this.val$value) ? Boolean.TRUE : null;
   }
}
