package com.thoughtworks.xstream.io.json;

import com.thoughtworks.xstream.io.AbstractDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.thoughtworks.xstream.io.xml.StaxWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import javax.xml.stream.XMLStreamException;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLOutputFactory;

public class JettisonMappedXmlDriver extends AbstractDriver {
   protected final MappedXMLOutputFactory mof;
   protected final MappedXMLInputFactory mif;
   protected final MappedNamespaceConvention convention;
   protected final boolean useSerializeAsArray;
   private static final Method setRootElementArrayWrapper;

   public JettisonMappedXmlDriver() {
      this(null);
   }

   public JettisonMappedXmlDriver(Configuration config) {
      this(config, true);
   }

   public JettisonMappedXmlDriver(Configuration config, boolean useSerializeAsArray) {
      if (config == null) {
         config = new Configuration();

         try {
            if (setRootElementArrayWrapper != null) {
               try {
                  setRootElementArrayWrapper.invoke(config, Boolean.FALSE);
               } catch (IllegalAccessException e) {
                  throw new StreamException("Cannot turn off Jettison wrapper for root element array", e);
               } catch (InvocationTargetException e) {
                  throw new StreamException("Cannot turn off Jettison wrapper for root element array", e);
               }
            }
         } catch (Error var6) {
         }
      }

      this.mof = new MappedXMLOutputFactory(config);
      this.mif = new MappedXMLInputFactory(config);
      this.convention = new MappedNamespaceConvention(config);
      this.useSerializeAsArray = useSerializeAsArray;
   }

   public HierarchicalStreamReader createReader(Reader reader) {
      try {
         return new StaxReader(new QNameMap(), this.mif.createXMLStreamReader(reader), this.getNameCoder());
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(InputStream input) {
      try {
         return new StaxReader(new QNameMap(), this.mif.createXMLStreamReader(input), this.getNameCoder());
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(URL in) {
      InputStream instream = null;

      try {
         instream = in.openStream();
         return new StaxReader(new QNameMap(), this.mif.createXMLStreamReader(in.toExternalForm(), instream), this.getNameCoder());
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      } finally {
         if (instream != null) {
            try {
               instream.close();
            } catch (IOException var12) {
            }
         }
      }
   }

   public HierarchicalStreamReader createReader(File in) {
      InputStream instream = null;

      try {
         instream = new FileInputStream(in);
         return new StaxReader(new QNameMap(), this.mif.createXMLStreamReader(in.toURI().toASCIIString(), instream), this.getNameCoder());
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      } finally {
         if (instream != null) {
            try {
               instream.close();
            } catch (IOException var12) {
            }
         }
      }
   }

   public HierarchicalStreamWriter createWriter(Writer writer) {
      try {
         return this.useSerializeAsArray
            ? new JettisonStaxWriter(new QNameMap(), this.mof.createXMLStreamWriter(writer), this.getNameCoder(), this.convention)
            : new StaxWriter(new QNameMap(), this.mof.createXMLStreamWriter(writer), this.getNameCoder());
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamWriter createWriter(OutputStream output) {
      try {
         return this.useSerializeAsArray
            ? new JettisonStaxWriter(new QNameMap(), this.mof.createXMLStreamWriter(output), this.getNameCoder(), this.convention)
            : new StaxWriter(new QNameMap(), this.mof.createXMLStreamWriter(output), this.getNameCoder());
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   static {
      Method method;
      try {
         method = Configuration.class.getDeclaredMethod("setRootElementArrayWrapper", boolean.class);
      } catch (NoSuchMethodException e) {
         method = null;
      }

      setRootElementArrayWrapper = method;
   }
}
