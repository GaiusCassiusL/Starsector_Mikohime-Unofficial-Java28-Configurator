package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.AbstractReader;
import com.thoughtworks.xstream.io.naming.NameCoder;

/** @deprecated */
public abstract class AbstractXmlReader extends AbstractReader {
   protected AbstractXmlReader() {
      this(new XmlFriendlyNameCoder());
   }

   /** @deprecated */
   protected AbstractXmlReader(XmlFriendlyReplacer replacer) {
      this((NameCoder)replacer);
   }

   protected AbstractXmlReader(NameCoder nameCoder) {
      super(nameCoder);
   }

   /** @deprecated */
   public String unescapeXmlName(String name) {
      return this.decodeNode(name);
   }

   /** @deprecated */
   protected String escapeXmlName(String name) {
      return this.encodeNode(name);
   }
}
