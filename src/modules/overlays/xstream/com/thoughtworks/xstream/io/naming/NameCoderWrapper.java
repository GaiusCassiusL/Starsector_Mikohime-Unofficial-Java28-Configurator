package com.thoughtworks.xstream.io.naming;

public class NameCoderWrapper implements NameCoder {
   private final NameCoder wrapped;

   public NameCoderWrapper(NameCoder inner) {
      this.wrapped = inner;
   }

   public String decodeAttribute(String attributeName) {
      return this.wrapped.decodeAttribute(attributeName);
   }

   public String decodeNode(String nodeName) {
      return this.wrapped.decodeNode(nodeName);
   }

   public String encodeAttribute(String name) {
      return this.wrapped.encodeAttribute(name);
   }

   public String encodeNode(String name) {
      return this.wrapped.encodeNode(name);
   }
}
