package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

public class TraxSource extends SAXSource {
   public static final String XSTREAM_FEATURE = "http://com.thoughtworks.xstream/XStreamSource/feature";
   private XMLReader xmlReader = null;
   private XStream xstream = null;
   private List source = null;

   public TraxSource() {
      super(new InputSource());
   }

   public TraxSource(Object source) {
      super(new InputSource());
      this.setSource(source);
   }

   public TraxSource(Object source, XStream xstream) {
      super(new InputSource());
      this.setSource(source);
      this.setXStream(xstream);
   }

   public TraxSource(List source) {
      super(new InputSource());
      this.setSourceAsList(source);
   }

   public TraxSource(List source, XStream xstream) {
      super(new InputSource());
      this.setSourceAsList(source);
      this.setXStream(xstream);
   }

   public void setInputSource(InputSource inputSource) {
      throw new UnsupportedOperationException();
   }

   public void setXMLReader(XMLReader reader) {
      this.createXMLReader(reader);
   }

   public XMLReader getXMLReader() {
      if (this.xmlReader == null) {
         this.createXMLReader(null);
      }

      return this.xmlReader;
   }

   public void setXStream(XStream xstream) {
      if (xstream == null) {
         throw new IllegalArgumentException("xstream");
      }

      this.xstream = xstream;
      this.configureXMLReader();
   }

   public void setSource(Object obj) {
      if (obj == null) {
         throw new IllegalArgumentException("obj");
      }

      List list = new ArrayList(1);
      list.add(obj);
      this.setSourceAsList(list);
   }

   public void setSourceAsList(List list) {
      if (list != null && !list.isEmpty()) {
         this.source = list;
         this.configureXMLReader();
      } else {
         throw new IllegalArgumentException("list");
      }
   }

   private void createXMLReader(XMLReader filterChain) {
      if (filterChain == null) {
         this.xmlReader = new SaxWriter();
      } else {
         if (!(filterChain instanceof XMLFilter)) {
            throw new UnsupportedOperationException();
         }

         XMLFilter filter = (XMLFilter)filterChain;

         while (filter.getParent() instanceof XMLFilter) {
            filter = (XMLFilter & XMLFilter)filter.getParent();
         }

         if (!(filter.getParent() instanceof SaxWriter)) {
            filter.setParent(new SaxWriter());
         }

         this.xmlReader = filterChain;
      }

      this.configureXMLReader();
   }

   private void configureXMLReader() {
      if (this.xmlReader != null) {
         try {
            if (this.xstream != null) {
               this.xmlReader.setProperty("http://com.thoughtworks.xstream/sax/property/configured-xstream", this.xstream);
            }

            if (this.source != null) {
               this.xmlReader.setProperty("http://com.thoughtworks.xstream/sax/property/source-object-list", this.source);
            }
         } catch (SAXException e) {
            throw new IllegalArgumentException(e.getMessage());
         }
      }
   }
}
