package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.core.Caching;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OuterClassMapper extends MapperWrapper implements Caching {
   private static final String[] EMPTY_NAMES = new String[0];
   private final String alias;
   private final Map innerFields;

   public OuterClassMapper(Mapper wrapped) {
      this(wrapped, "outer-class");
   }

   public OuterClassMapper(Mapper wrapped, String alias) {
      super(wrapped);
      this.alias = alias;
      this.innerFields = Collections.synchronizedMap(new HashMap());
      this.innerFields.put(Object.class.getName(), EMPTY_NAMES);
   }

   public String serializedMember(Class type, String memberName) {
      if (memberName.startsWith("this$")) {
         String[] innerFieldNames = this.getInnerFieldNames(type);

         for (int i = 0; i < innerFieldNames.length; i++) {
            if (innerFieldNames[i].equals(memberName)) {
               return i == 0 ? this.alias : this.alias + '-' + i;
            }
         }
      }

      return super.serializedMember(type, memberName);
   }

   public String realMember(Class type, String serialized) {
      if (serialized.startsWith(this.alias)) {
         int idx = -1;
         int len = this.alias.length();
         if (len == serialized.length()) {
            idx = 0;
         } else if (serialized.length() > len + 1 && serialized.charAt(len) == '-') {
            idx = Integer.valueOf(serialized.substring(len + 1));
         }

         if (idx >= 0) {
            String[] innerFieldNames = this.getInnerFieldNames(type);
            if (idx < innerFieldNames.length) {
               return innerFieldNames[idx];
            }
         }
      }

      return super.realMember(type, serialized);
   }

   private String[] getInnerFieldNames(Class type) {
      String[] innerFieldNames = (String[])this.innerFields.get(type.getName());
      if (innerFieldNames == null) {
         innerFieldNames = this.getInnerFieldNames(type.getSuperclass());
         Field[] declaredFields = type.getDeclaredFields();

         for (int i = 0; i < declaredFields.length; i++) {
            Field field = declaredFields[i];
            if (field.getName().startsWith("this$")) {
               String[] temp = new String[innerFieldNames.length + 1];
               System.arraycopy(innerFieldNames, 0, temp, 0, innerFieldNames.length);
               innerFieldNames = temp;
               innerFieldNames[innerFieldNames.length - 1] = field.getName();
            }
         }

         this.innerFields.put(type.getName(), innerFieldNames);
      }

      return innerFieldNames;
   }

   public void flushCache() {
      this.innerFields.keySet().retainAll(Collections.singletonList(Object.class.getName()));
   }
}
