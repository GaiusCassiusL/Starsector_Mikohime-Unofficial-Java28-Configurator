package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

public class StandardStaxDriver extends StaxDriver {
   public StandardStaxDriver() {
   }

   /** @deprecated */
   public StandardStaxDriver(QNameMap qnameMap, XmlFriendlyNameCoder nameCoder) {
      super(qnameMap, nameCoder);
   }

   public StandardStaxDriver(QNameMap qnameMap, NameCoder nameCoder) {
      super(qnameMap, nameCoder);
   }

   public StandardStaxDriver(QNameMap qnameMap) {
      super(qnameMap);
   }

   /** @deprecated */
   public StandardStaxDriver(XmlFriendlyNameCoder nameCoder) {
      super(nameCoder);
   }

   public StandardStaxDriver(NameCoder nameCoder) {
      super(nameCoder);
   }

   protected XMLInputFactory createInputFactory() {
      Exception exception = null;

      try {
         Class staxInputFactory = JVM.getStaxInputFactory();
         if (staxInputFactory != null) {
            XMLInputFactory instance = (XMLInputFactory)staxInputFactory.newInstance();
            instance.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
            return instance;
         }

         throw new StreamException("Java runtime has no standard XMLInputFactory implementation.", exception);
      } catch (InstantiationException e) {
         exception = e;
      } catch (IllegalAccessException e) {
         exception = e;
      } catch (ClassNotFoundException e) {
         exception = e;
      }

      throw new StreamException("Cannot create standard XMLInputFactory instance of Java runtime.", exception);
   }

   protected XMLOutputFactory createOutputFactory() {
      Exception exception = null;

      try {
         Class staxOutputFactory = JVM.getStaxOutputFactory();
         if (staxOutputFactory != null) {
            return (XMLOutputFactory)staxOutputFactory.newInstance();
         }

         throw new StreamException("Java runtime has no standard XMLOutputFactory implementation.", exception);
      } catch (InstantiationException e) {
         exception = e;
      } catch (IllegalAccessException e) {
         exception = e;
      } catch (ClassNotFoundException e) {
         exception = e;
      }

      throw new StreamException("Cannot create standard XMLOutputFactory instance of Java runtime.", exception);
   }
}
