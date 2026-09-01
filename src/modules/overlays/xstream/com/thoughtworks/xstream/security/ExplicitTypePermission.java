package com.thoughtworks.xstream.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExplicitTypePermission implements TypePermission {
   final Set names;

   public ExplicitTypePermission(Class[] types) {
      this(new ExplicitTypePermission$1(types).getNames());
   }

   public ExplicitTypePermission(String[] names) {
      this.names = names == null ? Collections.EMPTY_SET : new HashSet<>(Arrays.asList(names));
   }

   public boolean allows(Class type) {
      return type == null ? false : this.names.contains(type.getName());
   }
}
