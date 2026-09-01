package com.thoughtworks.xstream.converters.reflection;

import java.util.Map;
import java.util.TreeMap;

public class NativeFieldKeySorter implements FieldKeySorter {
   public Map sort(Class type, Map keyedByFieldKey) {
      Map map = new TreeMap(new NativeFieldKeySorter$1(this));
      map.putAll(keyedByFieldKey);
      return map;
   }
}
