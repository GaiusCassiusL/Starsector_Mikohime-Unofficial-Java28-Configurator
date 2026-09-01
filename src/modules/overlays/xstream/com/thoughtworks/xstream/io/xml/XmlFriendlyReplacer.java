package com.thoughtworks.xstream.io.xml;

/** @deprecated */
public class XmlFriendlyReplacer extends XmlFriendlyNameCoder {
   /** @deprecated */
   public XmlFriendlyReplacer() {
      this("_-", "__");
   }

   /** @deprecated */
   public XmlFriendlyReplacer(String dollarReplacement, String underscoreReplacement) {
      super(dollarReplacement, underscoreReplacement);
   }

   /** @deprecated */
   public String escapeName(String name) {
      return super.encodeNode(name);
   }

   /** @deprecated */
   public String unescapeName(String name) {
      return super.decodeNode(name);
   }
}
