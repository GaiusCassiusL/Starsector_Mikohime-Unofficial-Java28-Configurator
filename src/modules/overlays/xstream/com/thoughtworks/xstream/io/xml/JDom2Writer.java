package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.naming.NameCoder;
import org.jdom2.DefaultJDOMFactory;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;

public class JDom2Writer extends AbstractDocumentWriter {
   private final JDOMFactory documentFactory;

   public JDom2Writer(Element container, JDOMFactory factory, NameCoder nameCoder) {
      super(container, nameCoder);
      this.documentFactory = factory;
   }

   public JDom2Writer(Element container, JDOMFactory factory) {
      this(container, factory, new XmlFriendlyNameCoder());
   }

   public JDom2Writer(JDOMFactory factory, NameCoder nameCoder) {
      this(null, factory, nameCoder);
   }

   public JDom2Writer(JDOMFactory factory) {
      this(null, factory);
   }

   public JDom2Writer(Element container, NameCoder nameCoder) {
      this(container, new DefaultJDOMFactory(), nameCoder);
   }

   public JDom2Writer(Element container) {
      this(container, new DefaultJDOMFactory());
   }

   public JDom2Writer() {
      this(new DefaultJDOMFactory());
   }

   @Override
   protected Object createNode(String name) {
      Element element = this.documentFactory.element(this.encodeNode(name));
      Element parent = this.top();
      if (parent != null) {
         parent.addContent(element);
      }

      return element;
   }

   @Override
   public void setValue(String text) {
      this.top().addContent(this.documentFactory.text(text));
   }

   @Override
   public void addAttribute(String key, String value) {
      this.top().setAttribute(this.documentFactory.attribute(this.encodeAttribute(key), value));
   }

   private Element top() {
      return (Element)this.getCurrent();
   }
}
