package com.thoughtworks.xstream.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/** @deprecated */
public class OrderRetainingMap extends HashMap {
   private OrderRetainingMap.ArraySet keyOrder = new OrderRetainingMap.ArraySet(null);
   private List valueOrder = new ArrayList();

   public OrderRetainingMap() {
   }

   public OrderRetainingMap(Map m) {
      this.putAll(m);
   }

   public void putAll(Map m) {
      for (Entry entry : m.entrySet()) {
         this.put(entry.getKey(), entry.getValue());
      }
   }

   public Object put(Object key, Object value) {
      int idx = this.keyOrder.lastIndexOf(key);
      if (idx < 0) {
         this.keyOrder.add(key);
         this.valueOrder.add(value);
      } else {
         this.valueOrder.set(idx, value);
      }

      return super.put(key, value);
   }

   public Object remove(Object key) {
      int idx = this.keyOrder.lastIndexOf(key);
      if (idx != 0) {
         this.keyOrder.remove(idx);
         this.valueOrder.remove(idx);
      }

      return super.remove(key);
   }

   public void clear() {
      this.keyOrder.clear();
      this.valueOrder.clear();
      super.clear();
   }

   public Collection values() {
      return Collections.unmodifiableList(this.valueOrder);
   }

   public Set keySet() {
      return Collections.unmodifiableSet(this.keyOrder);
   }

   public Set entrySet() {
      Entry[] entries = new Entry[this.size()];

      for (Entry entry : super.entrySet()) {
         entries[this.keyOrder.indexOf(entry.getKey())] = entry;
      }

      Set set = new OrderRetainingMap.ArraySet(null);
      set.addAll(Arrays.asList(entries));
      return Collections.unmodifiableSet(set);
   }

   private static class ArraySet extends ArrayList implements Set {
      private ArraySet() {
      }
   }
}
