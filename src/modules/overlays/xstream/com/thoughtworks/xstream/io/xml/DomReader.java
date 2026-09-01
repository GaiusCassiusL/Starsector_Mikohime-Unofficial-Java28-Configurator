package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class DomReader extends AbstractDocumentReader {
   private Element currentElement;
   private StringBuffer textBuffer = new StringBuffer();
   private List childElements;
   private final FastStack childrenStack = new FastStack(16);

   public DomReader(Element rootElement) {
      this(rootElement, new XmlFriendlyNameCoder());
   }

   public DomReader(Document document) {
      this(document.getDocumentElement());
   }

   public DomReader(Element rootElement, NameCoder nameCoder) {
      super(rootElement, nameCoder);
      this.collectChildElements();
   }

   public DomReader(Document document, NameCoder nameCoder) {
      this(document.getDocumentElement(), nameCoder);
   }

   /** @deprecated */
   public DomReader(Element rootElement, XmlFriendlyReplacer replacer) {
      this(rootElement, (NameCoder)replacer);
   }

   /** @deprecated */
   public DomReader(Document document, XmlFriendlyReplacer replacer) {
      this(document.getDocumentElement(), (NameCoder)replacer);
   }

   public String getNodeName() {
      return this.decodeNode(this.currentElement.getTagName());
   }

   public String getValue() {
      NodeList childNodes = this.currentElement.getChildNodes();
      this.textBuffer.setLength(0);
      int length = childNodes.getLength();

      for (int i = 0; i < length; i++) {
         Node childNode = childNodes.item(i);
         if (childNode instanceof Text) {
            Text text = (Text)childNode;
            this.textBuffer.append(text.getData());
         }
      }

      return this.textBuffer.toString();
   }

   public String getAttribute(String name) {
      Attr attribute = this.currentElement.getAttributeNode(this.encodeAttribute(name));
      return attribute == null ? null : attribute.getValue();
   }

   public String getAttribute(int index) {
      return ((Attr)this.currentElement.getAttributes().item(index)).getValue();
   }

   public int getAttributeCount() {
      return this.currentElement.getAttributes().getLength();
   }

   public String getAttributeName(int index) {
      return this.decodeAttribute(((Attr)this.currentElement.getAttributes().item(index)).getName());
   }

   protected Object getParent() {
      return this.currentElement.getParentNode();
   }

   protected Object getChild(int index) {
      return this.childElements.get(index);
   }

   protected int getChildCount() {
      return this.childElements.size();
   }

   protected void reassignCurrentElement(Object current) {
      this.currentElement = (Element)current;
   }

   private void collectChildElements() {
      NodeList childNodes = this.currentElement.getChildNodes();
      this.childElements = new ArrayList();

      for (int i = 0; i < childNodes.getLength(); i++) {
         Node node = childNodes.item(i);
         if (node instanceof Element) {
            this.childElements.add(node);
         }
      }
   }

   public void moveDown() {
      super.moveDown();
      this.childrenStack.push(this.childElements);
      this.collectChildElements();
   }

   public void moveUp() {
      this.childElements = (List)this.childrenStack.pop();
      super.moveUp();
   }

   public String peekNextChild() {
      NodeList childNodes = this.currentElement.getChildNodes();

      for (int i = 0; i < childNodes.getLength(); i++) {
         Node node = childNodes.item(i);
         if (node instanceof Element) {
            return this.decodeNode(((Element)node).getTagName());
         }
      }

      return null;
   }
}
