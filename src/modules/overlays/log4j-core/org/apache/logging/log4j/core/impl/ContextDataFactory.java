package org.apache.logging.log4j.core.impl;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;

public class ContextDataFactory {
   private static final String CLASS_NAME = PropertiesUtil.getProperties().getStringProperty(Log4jPropertyKey.THREAD_CONTEXT_DATA_CLASS_NAME);
   private static final Class<? extends StringMap> CACHED_CLASS = createCachedClass(CLASS_NAME);
   private static final Constructor<?> DEFAULT_CONSTRUCTOR = createDefaultConstructor(CACHED_CLASS);
   private static final Constructor<?> INITIAL_CAPACITY_CONSTRUCTOR = createInitialCapacityConstructor(CACHED_CLASS);
   private static final StringMap EMPTY_STRING_MAP = createContextData(0);

   private static Class<? extends StringMap> createCachedClass(final String className) {
      if (className == null) {
         return null;
      }

      try {
         return Loader.loadClass(className).asSubclass(IndexedStringMap.class);
      } catch (Exception any) {
         return null;
      }
   }

   private static Constructor<?> createDefaultConstructor(final Class<? extends StringMap> cachedClass) {
      if (cachedClass == null) {
         return null;
      }

      try {
         return cachedClass.getConstructor();
      } catch (NoSuchMethodException | IllegalAccessError ignored) {
         return null;
      }
   }

   private static Constructor<?> createInitialCapacityConstructor(final Class<? extends StringMap> cachedClass) {
      if (cachedClass == null) {
         return null;
      }

      try {
         return cachedClass.getConstructor(int.class);
      } catch (NoSuchMethodException | IllegalAccessError ignored) {
         return null;
      }
   }

   public static StringMap createContextData() {
      if (DEFAULT_CONSTRUCTOR == null) {
         return new SortedArrayStringMap();
      }

      try {
         return (IndexedStringMap)DEFAULT_CONSTRUCTOR.newInstance();
      } catch (Throwable ignored) {
         return new SortedArrayStringMap();
      }
   }

   public static StringMap createContextData(final int initialCapacity) {
      if (INITIAL_CAPACITY_CONSTRUCTOR == null) {
         return new SortedArrayStringMap(initialCapacity);
      }

      try {
         return (IndexedStringMap)INITIAL_CAPACITY_CONSTRUCTOR.newInstance(initialCapacity);
      } catch (Throwable ignored) {
         return new SortedArrayStringMap(initialCapacity);
      }
   }

   public static StringMap createContextData(final Map<String, String> context) {
      StringMap contextData = createContextData(context.size());

      for (Entry<String, String> entry : context.entrySet()) {
         contextData.putValue(entry.getKey(), entry.getValue());
      }

      return contextData;
   }

   public static StringMap createContextData(final ReadOnlyStringMap readOnlyStringMap) {
      StringMap contextData = createContextData(readOnlyStringMap.size());
      contextData.putAll(readOnlyStringMap);
      return contextData;
   }

   public static StringMap emptyFrozenContextData() {
      return EMPTY_STRING_MAP;
   }

   static {
      EMPTY_STRING_MAP.freeze();
   }
}
