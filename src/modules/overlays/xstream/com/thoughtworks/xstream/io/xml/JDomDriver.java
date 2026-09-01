package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class JDomDriver extends AbstractXmlDriver {
   public JDomDriver() {
      super(new XmlFriendlyNameCoder());
   }

   public JDomDriver(NameCoder nameCoder) {
      super(nameCoder);
   }

   /** @deprecated */
   public JDomDriver(XmlFriendlyReplacer replacer) {
      this((NameCoder)replacer);
   }

   public HierarchicalStreamReader createReader(Reader reader) {
      try {
         SAXBuilder builder = this.createBuilder();
         Document document = builder.build(reader);
         return new JDomReader(document, this.getNameCoder());
      } catch (IOException e) {
         throw new StreamException(e);
      } catch (JDOMException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(InputStream in) {
      try {
         SAXBuilder builder = this.createBuilder();
         Document document = builder.build(in);
         return new JDomReader(document, this.getNameCoder());
      } catch (IOException e) {
         throw new StreamException(e);
      } catch (JDOMException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(URL in) {
      try {
         SAXBuilder builder = this.createBuilder();
         Document document = builder.build(in);
         return new JDomReader(document, this.getNameCoder());
      } catch (IOException e) {
         throw new StreamException(e);
      } catch (JDOMException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(File in) {
      try {
         SAXBuilder builder = this.createBuilder();
         Document document = builder.build(in);
         return new JDomReader(document, this.getNameCoder());
      } catch (IOException e) {
         throw new StreamException(e);
      } catch (JDOMException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamWriter createWriter(Writer out) {
      return new PrettyPrintWriter(out, this.getNameCoder());
   }

   public HierarchicalStreamWriter createWriter(OutputStream out) {
      return new PrettyPrintWriter(new OutputStreamWriter(out));
   }

   protected SAXBuilder createBuilder() {
      SAXBuilder builder = new SAXBuilder();
      builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return builder;
   }
}
