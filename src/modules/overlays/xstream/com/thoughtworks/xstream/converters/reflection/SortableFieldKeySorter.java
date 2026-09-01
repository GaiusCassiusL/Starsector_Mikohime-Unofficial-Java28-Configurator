package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.core.Caching;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class SortableFieldKeySorter implements FieldKeySorter, Caching {
   private static final FieldKey[] EMPTY_FIELD_KEY_ARRAY = new FieldKey[0];
   private final Map map = new HashMap();

   public Map sort(Class type, Map keyedByFieldKey) {
      if (!this.map.containsKey(type)) {
         return keyedByFieldKey;
      }

      Map result = new OrderRetainingMap();
      FieldKey[] fieldKeys = keyedByFieldKey.keySet().toArray(EMPTY_FIELD_KEY_ARRAY);
      Arrays.sort(fieldKeys, (Comparator<? super FieldKey>)this.map.get(type));

      for (int i = 0; i < fieldKeys.length; i++) {
         result.put(fieldKeys[i], keyedByFieldKey.get(fieldKeys[i]));
      }

      return result;
   }

   public void registerFieldOrder(Class type, String[] fields) {
      this.map.put(type, new SortableFieldKeySorter.FieldComparator(type, fields));
   }

   public void flushCache() {
      this.map.clear();
   }

   private class FieldComparator implements Comparator {
      private final String[] fieldOrder;
      private final Class type;

      public FieldComparator(Class type, String[] fields) {
         this.type = type;
         this.fieldOrder = fields;
      }

      public int compare(String first, String second) {
         int firstPosition = -1;
         int secondPosition = -1;

         for (int i = 0; i < this.fieldOrder.length; i++) {
            if (this.fieldOrder[i].equals(first)) {
               firstPosition = i;
            }

            if (this.fieldOrder[i].equals(second)) {
               secondPosition = i;
            }
         }

         if (firstPosition != -1 && secondPosition != -1) {
            return firstPosition - secondPosition;
         }

         ConversionException exception = new ConversionException("Incomplete list of serialized fields for type");
         exception.add("sort-type", this.type.getName());
         throw exception;
      }

      public int compare(Object firstObject, Object secondObject) {
         FieldKey first = (FieldKey)firstObject;
         FieldKey second = (FieldKey)secondObject;
         return this.compare(first.getFieldName(), second.getFieldName());
      }
   }
}
