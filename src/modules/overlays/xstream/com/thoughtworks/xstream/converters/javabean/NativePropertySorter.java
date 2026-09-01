package com.thoughtworks.xstream.converters.javabean;

import java.util.Map;

public class NativePropertySorter implements PropertySorter {
   public Map sort(Class type, Map nameMap) {
      return nameMap;
   }
}
