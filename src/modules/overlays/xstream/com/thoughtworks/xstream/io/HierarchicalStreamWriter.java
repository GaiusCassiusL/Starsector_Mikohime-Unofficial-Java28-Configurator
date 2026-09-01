package com.thoughtworks.xstream.io;

public interface HierarchicalStreamWriter {
   void startNode(String var1);

   void addAttribute(String var1, String var2);

   void setValue(String var1);

   void endNode();

   void flush();

   void close();

   HierarchicalStreamWriter underlyingWriter();
}
