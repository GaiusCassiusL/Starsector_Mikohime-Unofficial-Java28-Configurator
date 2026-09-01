package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.naming.NameCoder;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

public class JDom2Reader extends AbstractDocumentReader {
   private Element currentElement;

   public JDom2Reader(Element root) {
      super(root);
   }

   public JDom2Reader(Document document) {
      super(document.getRootElement());
   }

   public JDom2Reader(Element root, NameCoder nameCoder) {
      super(root, nameCoder);
   }

   public JDom2Reader(Document document, NameCoder nameCoder) {
      super(document.getRootElement(), nameCoder);
   }

   @Override
   protected void reassignCurrentElement(Object current) {
      this.currentElement = (Element)current;
   }

   @Override
   protected Object getParent() {
      return this.currentElement.getParentElement();
   }

   @Override
   protected Object getChild(int index) {
      return this.currentElement.getChildren().get(index);
   }

   @Override
   protected int getChildCount() {
      return this.currentElement.getChildren().size();
   }

   @Override
   public String getNodeName() {
      return this.decodeNode(this.currentElement.getName());
   }

   @Override
   public String getValue() {
      return this.currentElement.getText();
   }

   @Override
   public String getAttribute(String name) {
      return this.currentElement.getAttributeValue(this.encodeAttribute(name));
   }

   @Override
   public String getAttribute(int index) {
      return ((Attribute)this.currentElement.getAttributes().get(index)).getValue();
   }

   @Override
   public int getAttributeCount() {
      return this.currentElement.getAttributes().size();
   }

   @Override
   public String getAttributeName(int index) {
      return this.decodeAttribute(((Attribute)this.currentElement.getAttributes().get(index)).getQualifiedName());
   }

   @Override
   public String peekNextChild() {
      List list = this.currentElement.getChildren();
      return null != list && !list.isEmpty() ? this.decodeNode(((Element)list.get(0)).getName()) : null;
   }
}
