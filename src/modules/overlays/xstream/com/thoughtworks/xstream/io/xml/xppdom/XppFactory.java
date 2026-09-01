package com.thoughtworks.xstream.io.xml.xppdom;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class XppFactory {
   public static XmlPullParser createDefaultParser() throws XmlPullParserException {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      return factory.newPullParser();
   }

   public static XppDom buildDom(String xml) throws XmlPullParserException, IOException {
      return buildDom(new StringReader(xml));
   }

   public static XppDom buildDom(Reader r) throws XmlPullParserException, IOException {
      XmlPullParser parser = createDefaultParser();
      parser.setInput(r);
      return XppDom.build(parser);
   }

   public static XppDom buildDom(InputStream in, String encoding) throws XmlPullParserException, IOException {
      XmlPullParser parser = createDefaultParser();
      parser.setInput(in, encoding);
      return XppDom.build(parser);
   }
}
