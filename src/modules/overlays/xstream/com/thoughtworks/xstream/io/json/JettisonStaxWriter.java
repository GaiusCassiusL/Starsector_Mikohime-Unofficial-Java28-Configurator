package com.thoughtworks.xstream.io.json;

import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;

public class JettisonStaxWriter extends StaxWriter {
   private final MappedNamespaceConvention convention;
   private final List stack = new ArrayList();

   public JettisonStaxWriter(
      QNameMap qnameMap,
      XMLStreamWriter out,
      boolean writeEnclosingDocument,
      boolean namespaceRepairingMode,
      NameCoder nameCoder,
      MappedNamespaceConvention convention
   ) throws XMLStreamException {
      super(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode, nameCoder);
      this.convention = convention;
   }

   /** @deprecated */
   public JettisonStaxWriter(
      QNameMap qnameMap,
      XMLStreamWriter out,
      boolean writeEnclosingDocument,
      boolean namespaceRepairingMode,
      XmlFriendlyReplacer replacer,
      MappedNamespaceConvention convention
   ) throws XMLStreamException {
      this(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode, (NameCoder)replacer, convention);
   }

   public JettisonStaxWriter(
      QNameMap qnameMap, XMLStreamWriter out, boolean writeEnclosingDocument, boolean namespaceRepairingMode, MappedNamespaceConvention convention
   ) throws XMLStreamException {
      super(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode);
      this.convention = convention;
   }

   public JettisonStaxWriter(QNameMap qnameMap, XMLStreamWriter out, MappedNamespaceConvention convention) throws XMLStreamException {
      super(qnameMap, out);
      this.convention = convention;
   }

   public JettisonStaxWriter(QNameMap qnameMap, XMLStreamWriter out, NameCoder nameCoder, MappedNamespaceConvention convention) throws XMLStreamException {
      super(qnameMap, out, nameCoder);
      this.convention = convention;
   }

   public void startNode(String name, Class clazz) {
      XMLStreamWriter out = this.getXMLStreamWriter();
      String key = "";
      if (clazz != null
         && out instanceof AbstractXMLStreamWriter
         && (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz) || clazz.isArray())) {
         QName qname = this.getQNameMap().getQName(this.encodeNode(name));
         String prefix = qname.getPrefix();
         String uri = qname.getNamespaceURI();
         key = this.convention.createKey(prefix, uri, qname.getLocalPart());
         if (!JVM.is15()) {
            ArrayList serializedAsArrays = ((AbstractXMLStreamWriter)out).getSerializedAsArrays();
            if (!serializedAsArrays.contains(key)) {
               serializedAsArrays.add(key);
            }

            key = "";
         }
      }

      this.stack.add(key);
      super.startNode(name);
   }

   public void startNode(String name) {
      this.startNode(name, null);
   }

   public void endNode() {
      String key = (String)this.stack.remove(this.stack.size() - 1);
      if (key.length() == 0) {
         super.endNode();
      } else {
         XMLStreamWriter out = this.getXMLStreamWriter();
         ArrayList serializedAsArrays = ((AbstractXMLStreamWriter)out).getSerializedAsArrays();
         serializedAsArrays.add(key);
         super.endNode();
         serializedAsArrays.remove(key);
      }
   }
}
