package com.thoughtworks.xstream.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Map.Entry;

public class PresortedMap implements SortedMap {
   private final PresortedMap.ArraySet set;
   private final Comparator comparator;

   public PresortedMap() {
      this(null, new PresortedMap.ArraySet(null));
   }

   public PresortedMap(Comparator comparator) {
      this(comparator, new PresortedMap.ArraySet(null));
   }

   private PresortedMap(Comparator comparator, PresortedMap.ArraySet set) {
      this.comparator = comparator != null ? comparator : new PresortedMap.ArraySetComparator(set);
      this.set = set;
   }

   public Comparator comparator() {
      return this.comparator;
   }

   public Set entrySet() {
      return this.set;
   }

   public Object firstKey() {
      throw new UnsupportedOperationException();
   }

   public SortedMap headMap(Object toKey) {
      throw new UnsupportedOperationException();
   }

   public Set keySet() {
      Set keySet = new PresortedMap.ArraySet(null);

      for (Entry entry : this.set) {
         keySet.add(entry.getKey());
      }

      return keySet;
   }

   public Object lastKey() {
      throw new UnsupportedOperationException();
   }

   public SortedMap subMap(Object fromKey, Object toKey) {
      throw new UnsupportedOperationException();
   }

   public SortedMap tailMap(Object fromKey) {
      throw new UnsupportedOperationException();
   }

   public Collection values() {
      Set values = new PresortedMap.ArraySet(null);

      for (Entry entry : this.set) {
         values.add(entry.getValue());
      }

      return values;
   }

   public void clear() {
      throw new UnsupportedOperationException();
   }

   public boolean containsKey(Object key) {
      return false;
   }

   public boolean containsValue(Object value) {
      throw new UnsupportedOperationException();
   }

   public Object get(Object key) {
      throw new UnsupportedOperationException();
   }

   public boolean isEmpty() {
      return this.set.isEmpty();
   }

   public Object put(Object key, Object value) {
      this.set.add(new PresortedMap$1(this, key, value));
      return null;
   }

   public void putAll(Map m) {
      Iterator iter = m.entrySet().iterator();

      while (iter.hasNext()) {
         this.set.add(iter.next());
      }
   }

   public Object remove(Object key) {
      throw new UnsupportedOperationException();
   }

   public int size() {
      return this.set.size();
   }

   private static class ArraySet extends ArrayList implements Set {
      private ArraySet() {
      }
   }

   private static class ArraySetComparator implements Comparator {
      private final ArrayList list;
      private Entry[] array;

      ArraySetComparator(ArrayList list) {
         this.list = list;
      }

      public int compare(Object object1, Object object2) {
         if (this.array == null || this.list.size() != this.array.length) {
            Entry[] a = new Entry[this.list.size()];
            if (this.array != null) {
               System.arraycopy(this.array, 0, a, 0, this.array.length);
            }

            for (int i = this.array == null ? 0 : this.array.length; i < this.list.size(); i++) {
               a[i] = (Entry)this.list.get(i);
            }

            this.array = a;
         }

         int idx1 = Integer.MAX_VALUE;
         int idx2 = Integer.MAX_VALUE;

         for (int i = 0; i < this.array.length && (idx1 >= Integer.MAX_VALUE || idx2 >= Integer.MAX_VALUE); i++) {
            if (idx1 == Integer.MAX_VALUE && object1 == this.array[i].getKey()) {
               idx1 = i;
            }

            if (idx2 == Integer.MAX_VALUE && object2 == this.array[i].getKey()) {
               idx2 = i;
            }
         }

         return idx1 - idx2;
      }
   }
}
