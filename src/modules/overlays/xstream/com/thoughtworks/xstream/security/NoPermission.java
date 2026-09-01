package com.thoughtworks.xstream.security;

public class NoPermission implements TypePermission {
   private final TypePermission permission;

   public NoPermission(TypePermission permission) {
      this.permission = permission;
   }

   public boolean allows(Class type) {
      if (this.permission != null && !this.permission.allows(type)) {
         return false;
      } else {
         throw new ForbiddenClassException(type);
      }
   }
}
