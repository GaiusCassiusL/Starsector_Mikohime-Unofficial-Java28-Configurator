package com.thoughtworks.xstream.io.xml;

import com.bea.xml.stream.MXParserFactory;
import com.bea.xml.stream.XMLOutputFactoryBase;
import com.thoughtworks.xstream.io.naming.NameCoder;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

/** @deprecated */
public class BEAStaxDriver extends StaxDriver {
   /** @deprecated */
   public BEAStaxDriver() {
   }

   /** @deprecated */
   public BEAStaxDriver(QNameMap qnameMap, XmlFriendlyNameCoder nameCoder) {
      super(qnameMap, nameCoder);
   }

   /** @deprecated */
   public BEAStaxDriver(QNameMap qnameMap, NameCoder nameCoder) {
      super(qnameMap, nameCoder);
   }

   /** @deprecated */
   public BEAStaxDriver(QNameMap qnameMap) {
      super(qnameMap);
   }

   /** @deprecated */
   public BEAStaxDriver(XmlFriendlyNameCoder nameCoder) {
      super(nameCoder);
   }

   /** @deprecated */
   public BEAStaxDriver(NameCoder nameCoder) {
      super(nameCoder);
   }

   protected XMLInputFactory createInputFactory() {
      XMLInputFactory instance = new MXParserFactory();
      instance.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
      return instance;
   }

   protected XMLOutputFactory createOutputFactory() {
      return new XMLOutputFactoryBase();
   }
}
