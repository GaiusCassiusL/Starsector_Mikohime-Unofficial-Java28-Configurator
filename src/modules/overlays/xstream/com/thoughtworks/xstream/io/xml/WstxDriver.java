package com.thoughtworks.xstream.io.xml;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.thoughtworks.xstream.io.naming.NameCoder;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

public class WstxDriver extends StaxDriver {
   public WstxDriver() {
   }

   /** @deprecated */
   public WstxDriver(QNameMap qnameMap, XmlFriendlyNameCoder nameCoder) {
      super(qnameMap, nameCoder);
   }

   public WstxDriver(QNameMap qnameMap, NameCoder nameCoder) {
      super(qnameMap, nameCoder);
   }

   public WstxDriver(QNameMap qnameMap) {
      super(qnameMap);
   }

   /** @deprecated */
   public WstxDriver(XmlFriendlyNameCoder nameCoder) {
      super(nameCoder);
   }

   public WstxDriver(NameCoder nameCoder) {
      super(nameCoder);
   }

   protected XMLInputFactory createInputFactory() {
      XMLInputFactory instance = new WstxInputFactory();
      instance.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
      return instance;
   }

   protected XMLOutputFactory createOutputFactory() {
      return new WstxOutputFactory();
   }
}
