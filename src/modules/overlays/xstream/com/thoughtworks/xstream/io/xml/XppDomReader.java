package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;

public class XppDomReader extends AbstractDocumentReader {
   private XppDom currentElement;

   public XppDomReader(XppDom xppDom) {
      super(xppDom);
   }

   public XppDomReader(XppDom xppDom, NameCoder nameCoder) {
      super(xppDom, nameCoder);
   }

   /** @deprecated */
   public XppDomReader(XppDom xppDom, XmlFriendlyReplacer replacer) {
      this(xppDom, (NameCoder)replacer);
   }

   public String getNodeName() {
      return this.decodeNode(this.currentElement.getName());
   }

   public String getValue() {
      String text = null;

      try {
         text = this.currentElement.getValue();
      } catch (Exception var3) {
      }

      return text == null ? "" : text;
   }

   public String getAttribute(String attributeName) {
      return this.currentElement.getAttribute(this.encodeAttribute(attributeName));
   }

   public String getAttribute(int index) {
      return this.currentElement.getAttribute(this.currentElement.getAttributeNames()[index]);
   }

   public int getAttributeCount() {
      return this.currentElement.getAttributeNames().length;
   }

   public String getAttributeName(int index) {
      return this.decodeAttribute(this.currentElement.getAttributeNames()[index]);
   }

   protected Object getParent() {
      return this.currentElement.getParent();
   }

   protected Object getChild(int index) {
      return this.currentElement.getChild(index);
   }

   protected int getChildCount() {
      return this.currentElement.getChildCount();
   }

   protected void reassignCurrentElement(Object current) {
      this.currentElement = (XppDom)current;
   }

   public String peekNextChild() {
      return this.currentElement.getChildCount() == 0 ? null : this.decodeNode(this.currentElement.getChild(0).getName());
   }
}
