package com.thoughtworks.xstream.io.xml.xppdom;

import java.io.Reader;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

/** @deprecated */
public class Xpp3DomBuilder {
   /** @deprecated */
   public static Xpp3Dom build(Reader reader) throws Exception {
      XmlPullParser parser = new MXParser();
      parser.setInput(reader);

      try {
         return (Xpp3Dom)XppDom.build(parser);
      } finally {
         reader.close();
      }
   }
}
