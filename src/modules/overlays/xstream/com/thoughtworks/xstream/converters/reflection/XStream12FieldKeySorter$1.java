package com.thoughtworks.xstream.converters.reflection;

import java.util.Comparator;

class XStream12FieldKeySorter$1 implements Comparator {
   XStream12FieldKeySorter$1(XStream12FieldKeySorter this$0) {
      this.this$0 = this$0;
   }

   public int compare(Object o1, Object o2) {
      FieldKey fieldKey1 = (FieldKey)o1;
      FieldKey fieldKey2 = (FieldKey)o2;
      int i = fieldKey2.getDepth() - fieldKey1.getDepth();
      if (i == 0) {
         i = fieldKey1.getOrder() - fieldKey2.getOrder();
      }

      return i;
   }
}
