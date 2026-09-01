package com.thoughtworks.xstream.core.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MemberDictionary {
   private final Map types = new HashMap();

   public boolean add(Class definedIn, String member) {
      String className = definedIn == null ? null : definedIn.getName();
      Set members = (Set)this.types.get(className);
      if (members == null) {
         members = new HashSet();
         this.types.put(className, members);
      }

      return members.add(member);
   }

   public boolean contains(Class definedIn, String member) {
      String className = definedIn == null ? null : definedIn.getName();
      Set members = (Set)this.types.get(className);
      return members != null && members.contains(member);
   }
}
