package com.thoughtworks.xstream.core.util;

class WeakCache$2 implements WeakCache.Visitor {
   WeakCache$2(WeakCache this$0, int[] var2) {
      this.this$0 = this$0;
      this.val$i = var2;
   }

   public Object visit(Object element) {
      this.val$i[0]++;
      return null;
   }
}
