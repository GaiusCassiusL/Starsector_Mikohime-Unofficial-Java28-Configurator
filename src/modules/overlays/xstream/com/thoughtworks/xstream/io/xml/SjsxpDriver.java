package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.StreamException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

/** @deprecated */
public class SjsxpDriver extends StaxDriver {
   /** @deprecated */
   public SjsxpDriver() {
   }

   /** @deprecated */
   public SjsxpDriver(QNameMap qnameMap, XmlFriendlyNameCoder nameCoder) {
      super(qnameMap, nameCoder);
   }

   /** @deprecated */
   public SjsxpDriver(QNameMap qnameMap) {
      super(qnameMap);
   }

   /** @deprecated */
   public SjsxpDriver(XmlFriendlyNameCoder nameCoder) {
      super(nameCoder);
   }

   /** @deprecated */
   protected XMLInputFactory createInputFactory() {
      Exception exception = null;

      try {
         XMLInputFactory instance = (XMLInputFactory)Class.forName("com.sun.xml.internal.stream.XMLInputFactoryImpl").newInstance();
         instance.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
         return instance;
      } catch (InstantiationException e) {
         var6 = e;
      } catch (IllegalAccessException e) {
         var6 = e;
      } catch (ClassNotFoundException e) {
         var6 = e;
      }

      throw new StreamException("Cannot create SJSXP (Sun JDK 6 StAX) XMLInputFactory instance.", var6);
   }

   /** @deprecated */
   protected XMLOutputFactory createOutputFactory() {
      Exception exception = null;

      try {
         return (XMLOutputFactory)Class.forName("com.sun.xml.internal.stream.XMLOutputFactoryImpl").newInstance();
      } catch (InstantiationException e) {
         var6 = e;
      } catch (IllegalAccessException e) {
         var6 = e;
      } catch (ClassNotFoundException e) {
         var6 = e;
      }

      throw new StreamException("Cannot create SJSXP (Sun JDK 6 StAX) XMLOutputFactory instance.", var6);
   }
}
