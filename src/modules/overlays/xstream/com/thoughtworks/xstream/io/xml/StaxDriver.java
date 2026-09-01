package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

public class StaxDriver extends AbstractXmlDriver {
   private QNameMap qnameMap;
   private XMLInputFactory inputFactory;
   private XMLOutputFactory outputFactory;

   public StaxDriver() {
      this(new QNameMap());
   }

   public StaxDriver(QNameMap qnameMap) {
      this(qnameMap, new XmlFriendlyNameCoder());
   }

   public StaxDriver(QNameMap qnameMap, NameCoder nameCoder) {
      super(nameCoder);
      this.qnameMap = qnameMap;
   }

   public StaxDriver(NameCoder nameCoder) {
      this(new QNameMap(), nameCoder);
   }

   /** @deprecated */
   public StaxDriver(QNameMap qnameMap, XmlFriendlyReplacer replacer) {
      this(qnameMap, (NameCoder)replacer);
   }

   /** @deprecated */
   public StaxDriver(XmlFriendlyReplacer replacer) {
      this(new QNameMap(), (NameCoder)replacer);
   }

   public HierarchicalStreamReader createReader(Reader xml) {
      try {
         return this.createStaxReader(this.createParser(xml));
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(InputStream in) {
      try {
         return this.createStaxReader(this.createParser(in));
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(URL in) {
      try {
         InputStream stream = in.openStream();
         HierarchicalStreamReader reader = this.createStaxReader(this.createParser(new StreamSource(stream, in.toExternalForm())));
         return new StaxDriver$1(this, reader, stream);
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(File in) {
      try {
         InputStream stream = new FileInputStream(in);
         HierarchicalStreamReader reader = this.createStaxReader(this.createParser(new StreamSource(stream, in.toURI().toASCIIString())));
         return new StaxDriver$2(this, reader, stream);
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      } catch (FileNotFoundException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamWriter createWriter(Writer out) {
      try {
         return this.createStaxWriter(this.getOutputFactory().createXMLStreamWriter(out));
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamWriter createWriter(OutputStream out) {
      try {
         return this.createStaxWriter(this.getOutputFactory().createXMLStreamWriter(out));
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public AbstractPullReader createStaxReader(XMLStreamReader in) {
      return new StaxReader(this.qnameMap, in, this.getNameCoder());
   }

   public StaxWriter createStaxWriter(XMLStreamWriter out, boolean writeStartEndDocument) throws XMLStreamException {
      return new StaxWriter(this.qnameMap, out, writeStartEndDocument, this.isRepairingNamespace(), this.getNameCoder());
   }

   public StaxWriter createStaxWriter(XMLStreamWriter out) throws XMLStreamException {
      return this.createStaxWriter(out, true);
   }

   public QNameMap getQnameMap() {
      return this.qnameMap;
   }

   public void setQnameMap(QNameMap qnameMap) {
      this.qnameMap = qnameMap;
   }

   public XMLInputFactory getInputFactory() {
      if (this.inputFactory == null) {
         this.inputFactory = this.createInputFactory();
      }

      return this.inputFactory;
   }

   public XMLOutputFactory getOutputFactory() {
      if (this.outputFactory == null) {
         this.outputFactory = this.createOutputFactory();
      }

      return this.outputFactory;
   }

   public boolean isRepairingNamespace() {
      return Boolean.TRUE.equals(this.getOutputFactory().getProperty("javax.xml.stream.isRepairingNamespaces"));
   }

   public void setRepairingNamespace(boolean repairing) {
      this.getOutputFactory().setProperty("javax.xml.stream.isRepairingNamespaces", repairing ? Boolean.TRUE : Boolean.FALSE);
   }

   protected XMLStreamReader createParser(Reader xml) throws XMLStreamException {
      return this.getInputFactory().createXMLStreamReader(xml);
   }

   protected XMLStreamReader createParser(InputStream xml) throws XMLStreamException {
      return this.getInputFactory().createXMLStreamReader(xml);
   }

   protected XMLStreamReader createParser(Source source) throws XMLStreamException {
      return this.getInputFactory().createXMLStreamReader(source);
   }

   protected XMLInputFactory createInputFactory() {
      XMLInputFactory instance = XMLInputFactory.newInstance();
      instance.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
      return instance;
   }

   protected XMLOutputFactory createOutputFactory() {
      return XMLOutputFactory.newInstance();
   }
}
