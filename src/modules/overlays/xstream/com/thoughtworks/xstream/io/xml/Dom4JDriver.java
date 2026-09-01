package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.io.File;
import java.io.FilterWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;

public class Dom4JDriver extends AbstractXmlDriver {
   private DocumentFactory documentFactory;
   private OutputFormat outputFormat;

   public Dom4JDriver() {
      this(new XmlFriendlyNameCoder());
   }

   public Dom4JDriver(NameCoder nameCoder) {
      this(new DocumentFactory(), OutputFormat.createPrettyPrint(), nameCoder);
      this.outputFormat.setTrimText(false);
   }

   public Dom4JDriver(DocumentFactory documentFactory, OutputFormat outputFormat) {
      this(documentFactory, outputFormat, new XmlFriendlyNameCoder());
   }

   public Dom4JDriver(DocumentFactory documentFactory, OutputFormat outputFormat, NameCoder nameCoder) {
      super(nameCoder);
      this.documentFactory = documentFactory;
      this.outputFormat = outputFormat;
   }

   /** @deprecated */
   public Dom4JDriver(DocumentFactory documentFactory, OutputFormat outputFormat, XmlFriendlyReplacer replacer) {
      this(documentFactory, outputFormat, (NameCoder)replacer);
   }

   public DocumentFactory getDocumentFactory() {
      return this.documentFactory;
   }

   public void setDocumentFactory(DocumentFactory documentFactory) {
      this.documentFactory = documentFactory;
   }

   public OutputFormat getOutputFormat() {
      return this.outputFormat;
   }

   public void setOutputFormat(OutputFormat outputFormat) {
      this.outputFormat = outputFormat;
   }

   public HierarchicalStreamReader createReader(Reader text) {
      try {
         Document document = this.createReader().read(text);
         return new Dom4JReader(document, this.getNameCoder());
      } catch (DocumentException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(InputStream in) {
      try {
         Document document = this.createReader().read(in);
         return new Dom4JReader(document, this.getNameCoder());
      } catch (DocumentException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(URL in) {
      try {
         Document document = this.createReader().read(in);
         return new Dom4JReader(document, this.getNameCoder());
      } catch (DocumentException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(File in) {
      try {
         Document document = this.createReader().read(in);
         return new Dom4JReader(document, this.getNameCoder());
      } catch (DocumentException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamWriter createWriter(Writer out) {
      HierarchicalStreamWriter[] writer = new HierarchicalStreamWriter[1];
      FilterWriter filter = new Dom4JDriver$1(this, out, writer);
      writer[0] = new Dom4JXmlWriter(new XMLWriter(filter, this.outputFormat), this.getNameCoder());
      return writer[0];
   }

   public HierarchicalStreamWriter createWriter(OutputStream out) {
      String encoding = this.getOutputFormat() != null ? this.getOutputFormat().getEncoding() : null;
      Charset charset = encoding != null && Charset.isSupported(encoding) ? Charset.forName(encoding) : null;
      Writer writer = charset != null ? new OutputStreamWriter(out, charset) : new OutputStreamWriter(out);
      return this.createWriter(writer);
   }

   protected SAXReader createReader() throws DocumentException {
      SAXReader reader = new SAXReader();

      try {
         reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
         return reader;
      } catch (SAXException e) {
         throw new DocumentException("Cannot disable DOCTYPE processing", e);
      }
   }
}
