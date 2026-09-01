package com.thoughtworks.xstream.core.util;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;

public class WeakCache extends AbstractMap {
   private final Map map;

   public WeakCache() {
      this(new WeakHashMap());
   }

   public WeakCache(Map map) {
      this.map = map;
   }

   public Object get(Object key) {
      Reference reference = (Reference)this.map.get(key);
      return reference != null ? reference.get() : null;
   }

   public Object put(Object key, Object value) {
      Reference ref = this.map.put(key, this.createReference(value));
      return ref == null ? null : ref.get();
   }

   public Object remove(Object key) {
      Reference ref = (Reference)this.map.remove(key);
      return ref == null ? null : ref.get();
   }

   protected Reference createReference(Object value) {
      return new WeakReference<>(value);
   }

   public boolean containsValue(Object value) {
      Boolean result = (Boolean)this.iterate(new WeakCache$1(this, value), 0);
      return result == Boolean.TRUE;
   }

   public int size() {
      if (this.map.size() == 0) {
         return 0;
      }

      int[] i = new int[]{0};
      this.iterate(new WeakCache$2(this, i), 0);
      return i[0];
   }

   public Collection values() {
      Collection collection = new ArrayList();
      if (this.map.size() != 0) {
         this.iterate(new WeakCache$3(this, collection), 0);
      }

      return collection;
   }

   public Set entrySet() {
      Set set = new HashSet();
      if (this.map.size() != 0) {
         this.iterate(new WeakCache$4(this, set), 2);
      }

      return set;
   }

   private Object iterate(WeakCache.Visitor visitor, int type) {
      Object result = null;
      Iterator iter = this.map.entrySet().iterator();

      while (result == null && iter.hasNext()) {
         Entry entry = (Entry)iter.next();
         Reference reference = (Reference)entry.getValue();
         Object element = reference.get();
         if (element == null) {
            iter.remove();
         } else {
            switch (type) {
               case 0:
                  result = visitor.visit(element);
                  break;
               case 1:
                  result = visitor.visit(entry.getKey());
                  break;
               case 2:
                  result = visitor.visit(entry);
            }
         }
      }

      return result;
   }

   public boolean containsKey(Object key) {
      return this.map.containsKey(key);
   }

   public void clear() {
      this.map.clear();
   }

   public Set keySet() {
      return this.map.keySet();
   }

   public boolean equals(Object o) {
      return this.map.equals(o);
   }

   public int hashCode() {
      return this.map.hashCode();
   }

   public String toString() {
      return this.map.toString();
   }

   private interface Visitor {
      Object visit(Object var1);
   }
}
