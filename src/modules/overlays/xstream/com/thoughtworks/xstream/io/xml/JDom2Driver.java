package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.AbstractDriver;
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
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class JDom2Driver extends AbstractDriver {
   public JDom2Driver() {
      super(new XmlFriendlyNameCoder());
   }

   public JDom2Driver(NameCoder nameCoder) {
      super(nameCoder);
   }

   @Override
   public HierarchicalStreamReader createReader(Reader reader) {
      try {
         SAXBuilder builder = this.createBuilder();
         Document document = builder.build(reader);
         return new JDom2Reader(document, this.getNameCoder());
      } catch (IOException e) {
         throw new StreamException(e);
      } catch (JDOMException e) {
         throw new StreamException(e);
      }
   }

   @Override
   public HierarchicalStreamReader createReader(InputStream in) {
      try {
         SAXBuilder builder = this.createBuilder();
         Document document = builder.build(in);
         return new JDom2Reader(document, this.getNameCoder());
      } catch (IOException e) {
         throw new StreamException(e);
      } catch (JDOMException e) {
         throw new StreamException(e);
      }
   }

   @Override
   public HierarchicalStreamReader createReader(URL in) {
      try {
         SAXBuilder builder = this.createBuilder();
         Document document = builder.build(in);
         return new JDom2Reader(document, this.getNameCoder());
      } catch (IOException e) {
         throw new StreamException(e);
      } catch (JDOMException e) {
         throw new StreamException(e);
      }
   }

   @Override
   public HierarchicalStreamReader createReader(File in) {
      try {
         SAXBuilder builder = this.createBuilder();
         Document document = builder.build(in);
         return new JDom2Reader(document, this.getNameCoder());
      } catch (IOException e) {
         throw new StreamException(e);
      } catch (JDOMException e) {
         throw new StreamException(e);
      }
   }

   @Override
   public HierarchicalStreamWriter createWriter(Writer out) {
      return new PrettyPrintWriter(out, this.getNameCoder());
   }

   @Override
   public HierarchicalStreamWriter createWriter(OutputStream out) {
      return new PrettyPrintWriter(new OutputStreamWriter(out));
   }

   protected SAXBuilder createBuilder() {
      SAXBuilder builder = new SAXBuilder();
      builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return builder;
   }
}
