package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class StaxWriter extends AbstractXmlWriter {
   private final QNameMap qnameMap;
   private final XMLStreamWriter out;
   private final boolean writeEnclosingDocument;
   private boolean namespaceRepairingMode;
   private int tagDepth;

   public StaxWriter(QNameMap qnameMap, XMLStreamWriter out) throws XMLStreamException {
      this(qnameMap, out, true, true);
   }

   public StaxWriter(QNameMap qnameMap, XMLStreamWriter out, NameCoder nameCoder) throws XMLStreamException {
      this(qnameMap, out, true, true, nameCoder);
   }

   public StaxWriter(QNameMap qnameMap, XMLStreamWriter out, boolean writeEnclosingDocument, boolean namespaceRepairingMode, NameCoder nameCoder) throws XMLStreamException {
      super(nameCoder);
      this.qnameMap = qnameMap;
      this.out = out;
      this.writeEnclosingDocument = writeEnclosingDocument;
      this.namespaceRepairingMode = namespaceRepairingMode;
      if (writeEnclosingDocument) {
         out.writeStartDocument();
      }
   }

   public StaxWriter(QNameMap qnameMap, XMLStreamWriter out, boolean writeEnclosingDocument, boolean namespaceRepairingMode) throws XMLStreamException {
      this(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode, new XmlFriendlyNameCoder());
   }

   /** @deprecated */
   public StaxWriter(QNameMap qnameMap, XMLStreamWriter out, boolean writeEnclosingDocument, boolean namespaceRepairingMode, XmlFriendlyReplacer replacer) throws XMLStreamException {
      this(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode, (NameCoder)replacer);
   }

   public void flush() {
      try {
         this.out.flush();
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public void close() {
      try {
         this.out.close();
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public void addAttribute(String name, String value) {
      try {
         this.out.writeAttribute(this.encodeAttribute(name), value);
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public void endNode() {
      try {
         this.tagDepth--;
         this.out.writeEndElement();
         if (this.tagDepth == 0 && this.writeEnclosingDocument) {
            this.out.writeEndDocument();
         }
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public void setValue(String text) {
      try {
         this.out.writeCharacters(text);
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public void startNode(String name) {
      try {
         QName qname = this.qnameMap.getQName(this.encodeNode(name));
         String prefix = qname.getPrefix();
         String uri = qname.getNamespaceURI();
         boolean hasPrefix = prefix != null && prefix.length() > 0;
         boolean hasURI = uri != null && uri.length() > 0;
         boolean writeNamespace = false;
         if (hasURI) {
            if (hasPrefix) {
               String currentNamespace = this.out.getNamespaceContext().getNamespaceURI(prefix);
               if (currentNamespace == null || !currentNamespace.equals(uri)) {
                  writeNamespace = true;
               }
            } else {
               String defaultNamespace = this.out.getNamespaceContext().getNamespaceURI("");
               if (defaultNamespace == null || !defaultNamespace.equals(uri)) {
                  writeNamespace = true;
               }
            }
         }

         this.out.writeStartElement(prefix, qname.getLocalPart(), uri);
         if (hasPrefix) {
            this.out.setPrefix(prefix, uri);
         } else if (hasURI && writeNamespace) {
            this.out.setDefaultNamespace(uri);
         }

         if (hasURI && writeNamespace && !this.isNamespaceRepairingMode()) {
            if (hasPrefix) {
               this.out.writeNamespace(prefix, uri);
            } else {
               this.out.writeDefaultNamespace(uri);
            }
         }

         this.tagDepth++;
      } catch (XMLStreamException e) {
         throw new StreamException(e);
      }
   }

   public boolean isNamespaceRepairingMode() {
      return this.namespaceRepairingMode;
   }

   protected QNameMap getQNameMap() {
      return this.qnameMap;
   }

   protected XMLStreamWriter getXMLStreamWriter() {
      return this.out;
   }
}
