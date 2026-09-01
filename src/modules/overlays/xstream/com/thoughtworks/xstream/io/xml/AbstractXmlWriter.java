package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.AbstractWriter;
import com.thoughtworks.xstream.io.naming.NameCoder;

/** @deprecated */
public abstract class AbstractXmlWriter extends AbstractWriter implements XmlFriendlyWriter {
   protected AbstractXmlWriter() {
      this(new XmlFriendlyNameCoder());
   }

   /** @deprecated */
   protected AbstractXmlWriter(XmlFriendlyReplacer replacer) {
      this((NameCoder)replacer);
   }

   protected AbstractXmlWriter(NameCoder nameCoder) {
      super(nameCoder);
   }

   /** @deprecated */
   public String escapeXmlName(String name) {
      return super.encodeNode(name);
   }
}
