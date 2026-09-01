package org.apache.log4j;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.apache.logging.log4j.ThreadContext;

public final class MDC {
   private static final ThreadLocal<Map<String, Object>> localMap = new InheritableThreadLocal<Map<String, Object>>() {
      protected Map<String, Object> initialValue() {
         return new HashMap<>();
      }

      protected Map<String, Object> childValue(final Map<String, Object> parentValue) {
         return parentValue == null ? new HashMap<>() : new HashMap<>(parentValue);
      }
   };

   private MDC() {
   }

   public static void put(final String key, final String value) {
      localMap.get().put(key, value);
      ThreadContext.put(key, value);
   }

   public static void put(final String key, final Object value) {
      localMap.get().put(key, value);
      ThreadContext.put(key, value.toString());
   }

   public static Object get(final String key) {
      return localMap.get().get(key);
   }

   public static void remove(final String key) {
      localMap.get().remove(key);
      ThreadContext.remove(key);
   }

   public static void clear() {
      localMap.get().clear();
      ThreadContext.clearMap();
   }

   public static Hashtable<String, Object> getContext() {
      return new Hashtable<>(localMap.get());
   }
}
