package com.thoughtworks.xstream.core.util;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class PrioritizedList {
   private final Set set = new TreeSet();
   private int lowestPriority = Integer.MAX_VALUE;
   private int lastId = 0;

   public void add(Object item, int priority) {
      if (this.lowestPriority > priority) {
         this.lowestPriority = priority;
      }

      this.set.add(new PrioritizedList.PrioritizedItem(item, priority, ++this.lastId));
   }

   public Iterator iterator() {
      return new PrioritizedList.PrioritizedItemIterator(this.set.iterator());
   }

   private static class PrioritizedItem implements Comparable {
      final Object value;
      final int priority;
      final int id;

      public PrioritizedItem(Object value, int priority, int id) {
         this.value = value;
         this.priority = priority;
         this.id = id;
      }

      public int compareTo(Object o) {
         PrioritizedList.PrioritizedItem other = (PrioritizedList.PrioritizedItem)o;
         return this.priority != other.priority ? other.priority - this.priority : other.id - this.id;
      }

      public boolean equals(Object obj) {
         return this.id == ((PrioritizedList.PrioritizedItem)obj).id;
      }
   }

   private static class PrioritizedItemIterator implements Iterator {
      private Iterator iterator;

      public PrioritizedItemIterator(Iterator iterator) {
         this.iterator = iterator;
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      public boolean hasNext() {
         return this.iterator.hasNext();
      }

      public Object next() {
         return ((PrioritizedList.PrioritizedItem)this.iterator.next()).value;
      }
   }
}
