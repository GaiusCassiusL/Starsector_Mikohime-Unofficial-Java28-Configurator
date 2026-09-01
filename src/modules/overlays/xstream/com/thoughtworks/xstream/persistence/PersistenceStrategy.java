package com.thoughtworks.xstream.persistence;

import java.util.Iterator;

public interface PersistenceStrategy {
   Iterator iterator();

   int size();

   Object get(Object var1);

   Object put(Object var1, Object var2);

   Object remove(Object var1);
}
