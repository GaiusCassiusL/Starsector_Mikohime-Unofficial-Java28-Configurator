package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.naming.NameCoder;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

public class KXml2Driver extends AbstractXppDriver {
   public KXml2Driver() {
      super(new XmlFriendlyNameCoder());
   }

   public KXml2Driver(NameCoder nameCoder) {
      super(nameCoder);
   }

   protected XmlPullParser createParser() {
      return new KXmlParser();
   }
}
