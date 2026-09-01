package com.thoughtworks.xstream.io.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

public class QNameMap {
   private Map qnameToJava;
   private Map javaToQName;
   private String defaultPrefix = "";
   private String defaultNamespace = "";

   public String getJavaClassName(QName qname) {
      if (this.qnameToJava != null) {
         String answer = (String)this.qnameToJava.get(qname);
         if (answer != null) {
            return answer;
         }
      }

      return qname.getLocalPart();
   }

   public QName getQName(String javaClassName) {
      if (this.javaToQName != null) {
         QName answer = (QName)this.javaToQName.get(javaClassName);
         if (answer != null) {
            return answer;
         }
      }

      return new QName(this.defaultNamespace, javaClassName, this.defaultPrefix);
   }

   public synchronized void registerMapping(QName qname, String javaClassName) {
      if (this.javaToQName == null) {
         this.javaToQName = Collections.synchronizedMap(new HashMap());
      }

      if (this.qnameToJava == null) {
         this.qnameToJava = Collections.synchronizedMap(new HashMap());
      }

      this.javaToQName.put(javaClassName, qname);
      this.qnameToJava.put(qname, javaClassName);
   }

   public synchronized void registerMapping(QName qname, Class type) {
      this.registerMapping(qname, type.getName());
   }

   public String getDefaultNamespace() {
      return this.defaultNamespace;
   }

   public void setDefaultNamespace(String defaultNamespace) {
      this.defaultNamespace = defaultNamespace;
   }

   public String getDefaultPrefix() {
      return this.defaultPrefix;
   }

   public void setDefaultPrefix(String defaultPrefix) {
      this.defaultPrefix = defaultPrefix;
   }
}
