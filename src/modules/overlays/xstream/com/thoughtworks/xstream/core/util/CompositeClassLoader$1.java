package com.thoughtworks.xstream.core.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

class CompositeClassLoader$1 extends ArrayList {
   CompositeClassLoader$1(CompositeClassLoader this$0, int x0) {
      super(x0);
      this.this$0 = this$0;
   }

   public boolean addAll(Collection c) {
      boolean result = false;
      Iterator iter = c.iterator();

      while (iter.hasNext()) {
         result |= this.add(iter.next());
      }

      return result;
   }

   public boolean add(Object ref) {
      Object classLoader = ((WeakReference)ref).get();
      return classLoader != null ? super.add(classLoader) : false;
   }
}
