package com.thoughtworks.xstream.converters;

import java.util.Iterator;

public interface ErrorWriter {
   void add(String var1, String var2);

   void set(String var1, String var2);

   String get(String var1);

   Iterator keys();
}
