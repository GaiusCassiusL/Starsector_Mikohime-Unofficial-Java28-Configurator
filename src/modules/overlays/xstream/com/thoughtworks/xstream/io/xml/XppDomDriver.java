package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.naming.NameCoder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XppDomDriver extends AbstractXppDomDriver {
   public XppDomDriver() {
      super(new XmlFriendlyNameCoder());
   }

   public XppDomDriver(NameCoder nameCoder) {
      super(nameCoder);
   }

   /** @deprecated */
   public XppDomDriver(XmlFriendlyReplacer replacer) {
      super(replacer);
   }

   protected synchronized XmlPullParser createParser() throws XmlPullParserException {
      return XppDriver.createDefaultParser();
   }
}
