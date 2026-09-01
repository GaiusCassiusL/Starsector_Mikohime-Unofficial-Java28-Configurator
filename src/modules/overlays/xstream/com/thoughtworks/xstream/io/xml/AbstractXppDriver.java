package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.core.util.XmlHeaderAwareReader;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class AbstractXppDriver extends AbstractXmlDriver {
   public AbstractXppDriver(NameCoder nameCoder) {
      super(nameCoder);
   }

   public HierarchicalStreamReader createReader(Reader in) {
      try {
         return new XppReader(in, this.createParser(), this.getNameCoder());
      } catch (XmlPullParserException e) {
         throw new StreamException("Cannot create XmlPullParser", e);
      }
   }

   public HierarchicalStreamReader createReader(InputStream in) {
      try {
         return this.createReader(new XmlHeaderAwareReader(in));
      } catch (UnsupportedEncodingException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamWriter createWriter(Writer out) {
      return new PrettyPrintWriter(out, this.getNameCoder());
   }

   public HierarchicalStreamWriter createWriter(OutputStream out) {
      return this.createWriter(new OutputStreamWriter(out));
   }

   protected abstract XmlPullParser createParser() throws XmlPullParserException;
}
