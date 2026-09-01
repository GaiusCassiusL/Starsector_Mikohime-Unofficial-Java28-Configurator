package com.thoughtworks.xstream.io.naming;

public interface NameCoder {
   String encodeNode(String var1);

   String encodeAttribute(String var1);

   String decodeNode(String var1);

   String decodeAttribute(String var1);
}
