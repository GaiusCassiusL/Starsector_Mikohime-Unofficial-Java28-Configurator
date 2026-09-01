package com.thoughtworks.xstream.security;

import com.thoughtworks.xstream.mapper.Mapper;

public class NullPermission implements TypePermission {
   public static final TypePermission NULL = new NullPermission();

   public boolean allows(Class type) {
      return type == null || type == Mapper.Null.class;
   }
}
