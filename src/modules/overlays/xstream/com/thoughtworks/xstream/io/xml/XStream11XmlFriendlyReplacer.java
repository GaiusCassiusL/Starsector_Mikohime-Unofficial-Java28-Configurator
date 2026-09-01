package com.thoughtworks.xstream.io.xml;

/** @deprecated */
public class XStream11XmlFriendlyReplacer extends XmlFriendlyReplacer {
   public String decodeAttribute(String attributeName) {
      return attributeName;
   }

   public String decodeNode(String elementName) {
      return elementName;
   }

   public String unescapeName(String name) {
      return name;
   }
}
