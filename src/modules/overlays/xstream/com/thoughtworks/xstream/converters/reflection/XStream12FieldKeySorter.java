package com.thoughtworks.xstream.converters.reflection;

import java.util.Map;
import java.util.TreeMap;

public class XStream12FieldKeySorter implements FieldKeySorter {
   public Map sort(Class type, Map keyedByFieldKey) {
      Map map = new TreeMap(new XStream12FieldKeySorter$1(this));
      map.putAll(keyedByFieldKey);
      keyedByFieldKey.clear();
      keyedByFieldKey.putAll(map);
      return keyedByFieldKey;
   }
}
