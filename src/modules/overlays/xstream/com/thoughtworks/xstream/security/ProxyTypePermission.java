package com.thoughtworks.xstream.security;

import com.thoughtworks.xstream.mapper.DynamicProxyMapper;
import java.lang.reflect.Proxy;

public class ProxyTypePermission implements TypePermission {
   public static final TypePermission PROXIES = new ProxyTypePermission();

   public boolean allows(Class type) {
      return type != null && (Proxy.isProxyClass(type) || type == DynamicProxyMapper.DynamicProxy.class);
   }

   public int hashCode() {
      return 17;
   }

   public boolean equals(Object obj) {
      return obj != null && obj.getClass() == ProxyTypePermission.class;
   }
}
