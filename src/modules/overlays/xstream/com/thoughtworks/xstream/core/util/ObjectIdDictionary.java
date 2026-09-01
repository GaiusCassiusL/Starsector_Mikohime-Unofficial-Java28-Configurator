package com.thoughtworks.xstream.core.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class ObjectIdDictionary {
   private final Map map = new HashMap();
   private final ReferenceQueue queue = new ReferenceQueue();

   public void associateId(Object obj, Object id) {
      this.map.put(new ObjectIdDictionary.WeakIdWrapper(obj), id);
      this.cleanup();
   }

   public Object lookupId(Object obj) {
      return this.map.get(new ObjectIdDictionary.IdWrapper(obj));
   }

   public boolean containsId(Object item) {
      return this.map.containsKey(new ObjectIdDictionary.IdWrapper(item));
   }

   public void removeId(Object item) {
      this.map.remove(new ObjectIdDictionary.IdWrapper(item));
      this.cleanup();
   }

   public int size() {
      this.cleanup();
      return this.map.size();
   }

   private void cleanup() {
      ObjectIdDictionary.WeakIdWrapper wrapper;
      while ((wrapper = (ObjectIdDictionary.WeakIdWrapper)this.queue.poll()) != null) {
         this.map.remove(wrapper);
      }
   }

   private static class IdWrapper implements ObjectIdDictionary.Wrapper {
      private final Object obj;
      private final int hashCode;

      public IdWrapper(Object obj) {
         this.hashCode = System.identityHashCode(obj);
         this.obj = obj;
      }

      public int hashCode() {
         return this.hashCode;
      }

      public boolean equals(Object other) {
         return this.obj == ((ObjectIdDictionary.Wrapper)other).get();
      }

      public String toString() {
         return this.obj.toString();
      }

      public Object get() {
         return this.obj;
      }
   }

   private class WeakIdWrapper extends WeakReference implements ObjectIdDictionary.Wrapper {
      private final int hashCode;

      public WeakIdWrapper(Object obj) {
         super(obj, ObjectIdDictionary.this.queue);
         this.hashCode = System.identityHashCode(obj);
      }

      public int hashCode() {
         return this.hashCode;
      }

      public boolean equals(Object other) {
         return this.get() == ((ObjectIdDictionary.Wrapper)other).get();
      }

      public String toString() {
         Object obj = this.get();
         return obj == null ? "(null)" : obj.toString();
      }
   }

   private interface Wrapper {
      int hashCode();

      boolean equals(Object var1);

      String toString();

      Object get();
   }
}
