package com.thoughtworks.xstream.security;

public class ArrayTypePermission implements TypePermission {
   public static final TypePermission ARRAYS = new ArrayTypePermission();

   public boolean allows(Class type) {
      return type != null && type.isArray();
   }

   public int hashCode() {
      return 13;
   }

   public boolean equals(Object obj) {
      return obj != null && obj.getClass() == ArrayTypePermission.class;
   }
}
