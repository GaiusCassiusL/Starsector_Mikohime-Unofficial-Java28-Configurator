package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.AbstractDriver;
import com.thoughtworks.xstream.io.naming.NameCoder;

/** @deprecated */
public abstract class AbstractXmlDriver extends AbstractDriver {
   /** @deprecated */
   public AbstractXmlDriver() {
      this(new XmlFriendlyNameCoder());
   }

   public AbstractXmlDriver(NameCoder nameCoder) {
      super(nameCoder);
   }

   /** @deprecated */
   public AbstractXmlDriver(XmlFriendlyReplacer replacer) {
      this((NameCoder)replacer);
   }

   /** @deprecated */
   protected XmlFriendlyReplacer xmlFriendlyReplacer() {
      NameCoder nameCoder = this.getNameCoder();
      return nameCoder instanceof XmlFriendlyReplacer ? (XmlFriendlyReplacer)nameCoder : null;
   }
}
