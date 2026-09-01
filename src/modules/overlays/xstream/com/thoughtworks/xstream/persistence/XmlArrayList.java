package com.thoughtworks.xstream.persistence;

import java.util.AbstractList;

public class XmlArrayList extends AbstractList {
   private final XmlMap map;

   public XmlArrayList(PersistenceStrategy persistenceStrategy) {
      this.map = new XmlMap(persistenceStrategy);
   }

   public int size() {
      return this.map.size();
   }

   public Object set(int index, Object element) {
      this.rangeCheck(index);
      Object value = this.get(index);
      this.map.put(new Integer(index), element);
      return value;
   }

   public void add(int index, Object element) {
      int size = this.size();
      if (index < size + 1 && index >= 0) {
         int to = index != size ? index - 1 : index;

         for (int i = size; i > to; i--) {
            this.map.put(new Integer(i + 1), this.map.get(new Integer(i)));
         }

         this.map.put(new Integer(index), element);
      } else {
         throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
      }
   }

   private void rangeCheck(int index) {
      int size = this.size();
      if (index >= size || index < 0) {
         throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
      }
   }

   public Object get(int index) {
      this.rangeCheck(index);
      return this.map.get(new Integer(index));
   }

   public Object remove(int index) {
      int size = this.size();
      this.rangeCheck(index);
      Object value = this.map.get(new Integer(index));

      for (int i = index; i < size - 1; i++) {
         this.map.put(new Integer(i), this.map.get(new Integer(i + 1)));
      }

      this.map.remove(new Integer(size - 1));
      return value;
   }
}
