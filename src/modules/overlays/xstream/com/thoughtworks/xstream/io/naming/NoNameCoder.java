package com.thoughtworks.xstream.io.naming;

public class NoNameCoder implements NameCoder {
   public String decodeAttribute(String attributeName) {
      return attributeName;
   }

   public String decodeNode(String nodeName) {
      return nodeName;
   }

   public String encodeAttribute(String name) {
      return name;
   }

   public String encodeNode(String name) {
      return name;
   }
}
