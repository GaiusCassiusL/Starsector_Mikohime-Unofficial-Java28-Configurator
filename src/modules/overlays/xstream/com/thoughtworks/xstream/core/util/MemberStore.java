package com.thoughtworks.xstream.core.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MemberStore {
   private final Map types;
   private final boolean synced;

   public static MemberStore newInstance() {
      return new MemberStore(false);
   }

   public static MemberStore newSynchronizedInstance() {
      return new MemberStore(true);
   }

   private MemberStore(boolean synced) {
      this.synced = synced;
      this.types = synced ? Collections.synchronizedMap(new HashMap()) : new HashMap();
   }

   public Object put(Class definedIn, String member, Object value) {
      String className = definedIn == null ? null : definedIn.getName();
      Map store = (Map)this.types.get(className);
      if (store == null) {
         store = new HashMap();
         if (this.synced) {
            store = Collections.synchronizedMap(store);
         }

         this.types.put(className, store);
      }

      return store.put(member, value);
   }

   public Object get(Class definedIn, String member) {
      String className = definedIn == null ? null : definedIn.getName();
      Map store = (Map)this.types.get(className);
      return store != null ? store.get(member) : null;
   }

   public Set keySet() {
      return this.types.keySet();
   }
}
