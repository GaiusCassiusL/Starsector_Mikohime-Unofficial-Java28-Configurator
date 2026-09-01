package org.apache.logging.log4j.core.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.TriConsumer;

public class JdkMapAdapterStringMap implements StringMap {
   private static final String FROZEN = "Frozen collection cannot be modified";
   private static final Comparator<? super String> NULL_FIRST_COMPARATOR = (left, right) -> {
      if (left == null) {
         return -1;
      } else {
         return right == null ? 1 : left.compareTo(right);
      }
   };
   private final Map<String, String> map;
   private boolean immutable = false;
   private transient String[] sortedKeys;
   private static final TriConsumer<String, String, Map<String, String>> PUT_ALL = (key, value, stringStringMap) -> stringStringMap.put(key, value);

   public JdkMapAdapterStringMap() {
      this(new HashMap<>());
   }

   public JdkMapAdapterStringMap(final Map<String, String> map) {
      this.map = Objects.requireNonNull(map, "map");
   }

   public Map<String, String> toMap() {
      return this.map;
   }

   private void assertNotFrozen() {
      if (this.immutable) {
         throw new UnsupportedOperationException("Frozen collection cannot be modified");
      }
   }

   public boolean containsKey(final String key) {
      return this.map.containsKey(key);
   }

   public <V> void forEach(final BiConsumer<String, ? super V> action) {
      String[] keys = this.getSortedKeys();

      for (int i = 0; i < keys.length; i++) {
         action.accept(keys[i], this.map.get(keys[i]));
      }
   }

   public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
      String[] keys = this.getSortedKeys();

      for (int i = 0; i < keys.length; i++) {
         action.accept(keys[i], this.map.get(keys[i]), state);
      }
   }

   private String[] getSortedKeys() {
      if (this.sortedKeys == null) {
         this.sortedKeys = this.map.keySet().toArray(Strings.EMPTY_ARRAY);
         Arrays.sort(this.sortedKeys, NULL_FIRST_COMPARATOR);
      }

      return this.sortedKeys;
   }

   public <V> V getValue(final String key) {
      return (V)this.map.get(key);
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public int size() {
      return this.map.size();
   }

   public void clear() {
      if (!this.map.isEmpty()) {
         this.assertNotFrozen();
         this.map.clear();
         this.sortedKeys = null;
      }
   }

   public void freeze() {
      this.immutable = true;
   }

   public boolean isFrozen() {
      return this.immutable;
   }

   public void putAll(final ReadOnlyStringMap source) {
      this.assertNotFrozen();
      source.forEach(PUT_ALL, this.map);
      this.sortedKeys = null;
   }

   public void putValue(final String key, final Object value) {
      this.assertNotFrozen();
      this.map.put(key, value == null ? null : String.valueOf(value));
      this.sortedKeys = null;
   }

   public void remove(final String key) {
      if (this.map.containsKey(key)) {
         this.assertNotFrozen();
         this.map.remove(key);
         this.sortedKeys = null;
      }
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder(this.map.size() * 13);
      result.append('{');
      String[] keys = this.getSortedKeys();

      for (int i = 0; i < keys.length; i++) {
         if (i > 0) {
            result.append(", ");
         }

         result.append(keys[i]).append('=').append(this.map.get(keys[i]));
      }

      result.append('}');
      return result.toString();
   }

   @Override
   public boolean equals(final Object object) {
      if (object == this) {
         return true;
      }

      if (!(object instanceof JdkMapAdapterStringMap)) {
         return false;
      }

      JdkMapAdapterStringMap other = (JdkMapAdapterStringMap)object;
      return this.map.equals(other.map) && this.immutable == other.immutable;
   }

   @Override
   public int hashCode() {
      return this.map.hashCode() + (this.immutable ? 31 : 0);
   }
}
