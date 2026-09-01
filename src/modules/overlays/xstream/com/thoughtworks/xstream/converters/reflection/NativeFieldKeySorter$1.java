package com.thoughtworks.xstream.converters.reflection;

import java.util.Comparator;

class NativeFieldKeySorter$1 implements Comparator {
   NativeFieldKeySorter$1(NativeFieldKeySorter this$0) {
      this.this$0 = this$0;
   }

   public int compare(Object o1, Object o2) {
      FieldKey fieldKey1 = (FieldKey)o1;
      FieldKey fieldKey2 = (FieldKey)o2;
      int i = fieldKey1.getDepth() - fieldKey2.getDepth();
      if (i == 0) {
         i = fieldKey1.getOrder() - fieldKey2.getOrder();
      }

      return i;
   }
}
