package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.io.IOException;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultElement;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class Dom4JXmlWriter extends AbstractXmlWriter {
   private final XMLWriter writer;
   private final FastStack elementStack;
   private AttributesImpl attributes;
   private boolean started;
   private boolean children;

   public Dom4JXmlWriter(XMLWriter writer) {
      this(writer, new XmlFriendlyNameCoder());
   }

   public Dom4JXmlWriter(XMLWriter writer, NameCoder nameCoder) {
      super(nameCoder);
      this.writer = writer;
      this.elementStack = new FastStack(16);
      this.attributes = new AttributesImpl();

      try {
         writer.startDocument();
      } catch (SAXException e) {
         throw new StreamException(e);
      }
   }

   /** @deprecated */
   public Dom4JXmlWriter(XMLWriter writer, XmlFriendlyReplacer replacer) {
      this(writer, (NameCoder)replacer);
   }

   public void startNode(String name) {
      if (this.elementStack.size() > 0) {
         try {
            this.startElement();
         } catch (SAXException e) {
            throw new StreamException(e);
         }

         this.started = false;
      }

      this.elementStack.push(this.encodeNode(name));
      this.children = false;
   }

   public void setValue(String text) {
      char[] value = text.toCharArray();
      if (value.length > 0) {
         try {
            this.startElement();
            this.writer.characters(value, 0, value.length);
         } catch (SAXException e) {
            throw new StreamException(e);
         }

         this.children = true;
      }
   }

   public void addAttribute(String key, String value) {
      this.attributes.addAttribute("", "", this.encodeAttribute(key), "string", value);
   }

   public void endNode() {
      try {
         if (!this.children) {
            Element element = new DefaultElement((String)this.elementStack.pop());

            for (int i = 0; i < this.attributes.getLength(); i++) {
               element.addAttribute(this.attributes.getQName(i), this.attributes.getValue(i));
            }

            this.writer.write(element);
            this.attributes.clear();
            this.children = true;
            this.started = true;
         } else {
            this.startElement();
            this.writer.endElement("", "", (String)this.elementStack.pop());
         }
      } catch (SAXException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public void flush() {
      try {
         this.writer.flush();
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public void close() {
      try {
         this.writer.endDocument();
         this.writer.flush();
      } catch (SAXException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   private void startElement() throws SAXException {
      if (!this.started) {
         this.writer.startElement("", "", (String)this.elementStack.peek(), this.attributes);
         this.attributes.clear();
         this.started = true;
      }
   }
}
