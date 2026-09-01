package com.thoughtworks.xstream.security;

public class TypeHierarchyPermission implements TypePermission {
   private Class type;

   public TypeHierarchyPermission(Class type) {
      this.type = type;
   }

   public boolean allows(Class type) {
      return type == null ? false : this.type.isAssignableFrom(type);
   }
}
