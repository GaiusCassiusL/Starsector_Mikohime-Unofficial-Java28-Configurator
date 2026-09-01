package com.thoughtworks.xstream.core.util;

import java.lang.ref.Reference;
import java.util.Map.Entry;

class WeakCache$4$1 implements Entry {
   WeakCache$4$1(WeakCache$4 this$1, Entry var2) {
      this.this$1 = this$1;
      this.val$entry = var2;
   }

   public Object getKey() {
      return this.val$entry.getKey();
   }

   public Object getValue() {
      return ((Reference)this.val$entry.getValue()).get();
   }

   public Object setValue(Object value) {
      Reference reference = this.val$entry.setValue(WeakCache$4.access$000(this.this$1).createReference(value));
      return reference != null ? reference.get() : null;
   }
}
