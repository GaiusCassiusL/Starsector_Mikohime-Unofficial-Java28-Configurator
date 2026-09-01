package com.thoughtworks.xstream.converters.javabean;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class ComparingPropertySorter implements PropertySorter {
   private final Comparator comparator;

   public ComparingPropertySorter(Comparator propertyNameComparator) {
      this.comparator = propertyNameComparator;
   }

   public Map sort(Class type, Map nameMap) {
      TreeMap map = new TreeMap(this.comparator);
      map.putAll(nameMap);
      return map;
   }
}
