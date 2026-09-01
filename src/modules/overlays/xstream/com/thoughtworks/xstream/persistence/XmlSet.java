package com.thoughtworks.xstream.persistence;

import java.util.AbstractSet;
import java.util.Iterator;

public class XmlSet extends AbstractSet {
   private final XmlMap map;

   public XmlSet(PersistenceStrategy persistenceStrategy) {
      this.map = new XmlMap(persistenceStrategy);
   }

   public Iterator iterator() {
      return this.map.values().iterator();
   }

   public int size() {
      return this.map.size();
   }

   public boolean add(Object o) {
      if (this.map.containsValue(o)) {
         return false;
      }

      this.map.put(this.findEmptyKey(), o);
      return true;
   }

   private Long findEmptyKey() {
      long i = System.currentTimeMillis();

      while (this.map.containsKey(new Long(i))) {
         i++;
      }

      return new Long(i);
   }
}
