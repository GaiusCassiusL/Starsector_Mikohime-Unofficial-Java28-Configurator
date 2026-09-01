package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.core.JVM;
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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DomDriver extends AbstractXmlDriver {
   private final String encoding;
   private DocumentBuilderFactory documentBuilderFactory;

   public DomDriver() {
      this(null);
   }

   public DomDriver(String encoding) {
      this(encoding, new XmlFriendlyNameCoder());
   }

   public DomDriver(String encoding, NameCoder nameCoder) {
      super(nameCoder);
      this.encoding = encoding;
   }

   /** @deprecated */
   public DomDriver(String encoding, XmlFriendlyReplacer replacer) {
      this(encoding, (NameCoder)replacer);
   }

   public HierarchicalStreamReader createReader(Reader in) {
      return this.createReader(new InputSource(in));
   }

   public HierarchicalStreamReader createReader(InputStream in) {
      return this.createReader(new InputSource(in));
   }

   public HierarchicalStreamReader createReader(URL in) {
      return this.createReader(new InputSource(in.toExternalForm()));
   }

   public HierarchicalStreamReader createReader(File in) {
      return this.createReader(new InputSource(in.toURI().toASCIIString()));
   }

   private HierarchicalStreamReader createReader(InputSource source) {
      try {
         if (this.documentBuilderFactory == null) {
            synchronized (this) {
               if (this.documentBuilderFactory == null) {
                  this.documentBuilderFactory = this.createDocumentBuilderFactory();
               }
            }
         }

         DocumentBuilder documentBuilder = this.documentBuilderFactory.newDocumentBuilder();
         if (this.encoding != null) {
            source.setEncoding(this.encoding);
         }

         Document document = documentBuilder.parse(source);
         return new DomReader(document, this.getNameCoder());
      } catch (FactoryConfigurationError e) {
         throw new StreamException(e);
      } catch (ParserConfigurationException e) {
         throw new StreamException(e);
      } catch (SAXException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamWriter createWriter(Writer out) {
      return new PrettyPrintWriter(out, this.getNameCoder());
   }

   public HierarchicalStreamWriter createWriter(OutputStream out) {
      try {
         return this.createWriter(this.encoding != null ? new OutputStreamWriter(out, this.encoding) : new OutputStreamWriter(out));
      } catch (UnsupportedEncodingException e) {
         throw new StreamException(e);
      }
   }

   protected DocumentBuilderFactory createDocumentBuilderFactory() {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      if (JVM.isVersion(5)) {
         try {
            Method method = DocumentBuilderFactory.class.getMethod("setFeature", String.class, boolean.class);
            method.invoke(factory, "http://apache.org/xml/features/disallow-doctype-decl", Boolean.TRUE);
         } catch (NoSuchMethodException var4) {
         } catch (IllegalAccessException e) {
            throw new ObjectAccessException("Cannot set feature of DocumentBuilderFactory.", e);
         } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (JVM.isVersion(6) || cause instanceof ParserConfigurationException && cause.getMessage().indexOf("disallow-doctype-decl") < 0) {
               throw new StreamException(cause);
            }
         }
      }

      factory.setExpandEntityReferences(false);
      return factory;
   }
}
