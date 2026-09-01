package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.naming.NameCoder;
import io.github.xstream.mxparser.MXParser;
import org.xmlpull.v1.XmlPullParser;

public class MXParserDriver extends AbstractXppDriver {
   public MXParserDriver() {
      super(new XmlFriendlyNameCoder());
   }

   public MXParserDriver(NameCoder nameCoder) {
      super(nameCoder);
   }

   protected XmlPullParser createParser() {
      return new MXParser();
   }
}
