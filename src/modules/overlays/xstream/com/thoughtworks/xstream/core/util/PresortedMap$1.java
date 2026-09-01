package com.thoughtworks.xstream.core.util;

import java.util.Map.Entry;

class PresortedMap$1 implements Entry {
   PresortedMap$1(PresortedMap this$0, Object var2, Object var3) {
      this.this$0 = this$0;
      this.val$key = var2;
      this.val$value = var3;
   }

   public Object getKey() {
      return this.val$key;
   }

   public Object getValue() {
      return this.val$value;
   }

   public Object setValue(Object value) {
      throw new UnsupportedOperationException();
   }
}
