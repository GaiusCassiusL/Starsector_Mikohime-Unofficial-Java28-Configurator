package com.thoughtworks.xstream.core.util;

import java.lang.reflect.Constructor;
import java.util.Comparator;

final class DependencyInjectionFactory$1 implements Comparator {
   public int compare(Object o1, Object o2) {
      return ((Constructor)o2).getParameterTypes().length - ((Constructor)o1).getParameterTypes().length;
   }
}
