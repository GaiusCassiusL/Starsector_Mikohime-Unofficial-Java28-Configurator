package com.thoughtworks.xstream.mapper;

import java.lang.reflect.Proxy;

public class DynamicProxyMapper extends MapperWrapper {
   private String alias;

   public DynamicProxyMapper(Mapper wrapped) {
      this(wrapped, "dynamic-proxy");
   }

   public DynamicProxyMapper(Mapper wrapped, String alias) {
      super(wrapped);
      this.alias = alias;
   }

   public String getAlias() {
      return this.alias;
   }

   public void setAlias(String alias) {
      this.alias = alias;
   }

   public String serializedClass(Class type) {
      return Proxy.isProxyClass(type) ? this.alias : super.serializedClass(type);
   }

   public Class realClass(String elementName) {
      if (elementName.equals(this.alias)) {
         return DynamicProxyMapper.DynamicProxy.class;
      } else {
         return super.realClass(elementName);
      }
   }

   public static class DynamicProxy {
   }
}
