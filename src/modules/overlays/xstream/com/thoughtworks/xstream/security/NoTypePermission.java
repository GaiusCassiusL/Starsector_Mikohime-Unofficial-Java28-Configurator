package com.thoughtworks.xstream.security;

public class NoTypePermission implements TypePermission {
   public static final TypePermission NONE = new NoTypePermission();

   public boolean allows(Class type) {
      throw new ForbiddenClassException(type);
   }

   public int hashCode() {
      return 1;
   }

   public boolean equals(Object obj) {
      return obj != null && obj.getClass() == NoTypePermission.class;
   }
}
