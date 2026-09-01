package com.thoughtworks.xstream.security;

import net.sf.cglib.proxy.Proxy;

public class CGLIBProxyTypePermission implements TypePermission {
   public static final TypePermission PROXIES = new CGLIBProxyTypePermission();

   public boolean allows(Class type) {
      return type != null
         && type != Object.class
         && !type.isInterface()
         && (Proxy.isProxyClass(type) || type.getName().startsWith(Proxy.class.getPackage().getName() + "."));
   }

   public int hashCode() {
      return 19;
   }

   public boolean equals(Object obj) {
      return obj != null && obj.getClass() == CGLIBProxyTypePermission.class;
   }
}
