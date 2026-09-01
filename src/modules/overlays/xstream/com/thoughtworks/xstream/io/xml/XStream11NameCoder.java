package com.thoughtworks.xstream.io.xml;

public class XStream11NameCoder extends XmlFriendlyNameCoder {
   public String decodeAttribute(String attributeName) {
      return attributeName;
   }

   public String decodeNode(String elementName) {
      return elementName;
   }
}
