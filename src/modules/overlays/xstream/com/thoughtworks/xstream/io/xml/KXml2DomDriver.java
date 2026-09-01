package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.naming.NameCoder;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

public class KXml2DomDriver extends AbstractXppDomDriver {
   public KXml2DomDriver() {
      super(new XmlFriendlyNameCoder());
   }

   public KXml2DomDriver(NameCoder nameCoder) {
      super(nameCoder);
   }

   protected XmlPullParser createParser() {
      return new KXmlParser();
   }
}
