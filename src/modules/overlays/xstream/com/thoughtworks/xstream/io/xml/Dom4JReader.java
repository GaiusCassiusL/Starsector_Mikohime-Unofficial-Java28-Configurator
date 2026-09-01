package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.converters.ErrorWriter;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.util.List;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.Element;

public class Dom4JReader extends AbstractDocumentReader {
   private Element currentElement;

   public Dom4JReader(Branch branch) {
      this(branch instanceof Element ? (Element)branch : ((Document)branch).getRootElement());
   }

   public Dom4JReader(Element rootElement) {
      this(rootElement, new XmlFriendlyNameCoder());
   }

   public Dom4JReader(Document document) {
      this(document.getRootElement());
   }

   public Dom4JReader(Element rootElement, NameCoder nameCoder) {
      super(rootElement, nameCoder);
   }

   public Dom4JReader(Document document, NameCoder nameCoder) {
      this(document.getRootElement(), nameCoder);
   }

   /** @deprecated */
   public Dom4JReader(Element rootElement, XmlFriendlyReplacer replacer) {
      this(rootElement, (NameCoder)replacer);
   }

   /** @deprecated */
   public Dom4JReader(Document document, XmlFriendlyReplacer replacer) {
      this(document.getRootElement(), (NameCoder)replacer);
   }

   public String getNodeName() {
      return this.decodeNode(this.currentElement.getName());
   }

   public String getValue() {
      return this.currentElement.getText();
   }

   public String getAttribute(String name) {
      return this.currentElement.attributeValue(this.encodeAttribute(name));
   }

   public String getAttribute(int index) {
      return this.currentElement.attribute(index).getValue();
   }

   public int getAttributeCount() {
      return this.currentElement.attributeCount();
   }

   public String getAttributeName(int index) {
      return this.decodeAttribute(this.currentElement.attribute(index).getQualifiedName());
   }

   protected Object getParent() {
      return this.currentElement.getParent();
   }

   protected Object getChild(int index) {
      return this.currentElement.elements().get(index);
   }

   protected int getChildCount() {
      return this.currentElement.elements().size();
   }

   protected void reassignCurrentElement(Object current) {
      this.currentElement = (Element)current;
   }

   public String peekNextChild() {
      List list = this.currentElement.elements();
      return null != list && !list.isEmpty() ? this.decodeNode(((Element)list.get(0)).getName()) : null;
   }

   public void appendErrors(ErrorWriter errorWriter) {
      errorWriter.add("xpath", this.currentElement.getPath());
   }
}
