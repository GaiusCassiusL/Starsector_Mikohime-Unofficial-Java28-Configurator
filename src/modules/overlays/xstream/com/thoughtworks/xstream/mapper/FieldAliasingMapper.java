package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.core.util.MemberStore;
import java.util.regex.Pattern;

public class FieldAliasingMapper extends MapperWrapper {
   private final MemberStore fieldToAliasMap = MemberStore.newInstance();
   private final MemberStore aliasToFieldMap = MemberStore.newInstance();
   private final ElementIgnoringMapper elementIgnoringMapper;

   public FieldAliasingMapper(Mapper wrapped) {
      super(wrapped);
      this.elementIgnoringMapper = (ElementIgnoringMapper)this.lookupMapperOfType(ElementIgnoringMapper.class);
   }

   public void addFieldAlias(String alias, Class type, String fieldName) {
      this.fieldToAliasMap.put(type, fieldName, alias);
      this.aliasToFieldMap.put(type, alias, fieldName);
   }

   /** @deprecated */
   public void addFieldsToIgnore(Pattern pattern) {
      if (this.elementIgnoringMapper != null) {
         this.elementIgnoringMapper.addElementsToIgnore(pattern);
      }
   }

   /** @deprecated */
   public void omitField(Class definedIn, String fieldName) {
      if (this.elementIgnoringMapper != null) {
         this.elementIgnoringMapper.omitField(definedIn, fieldName);
      }
   }

   public String serializedMember(Class type, String memberName) {
      String alias = this.getMember(type, memberName, this.fieldToAliasMap);
      return alias == null ? super.serializedMember(type, memberName) : alias;
   }

   public String realMember(Class type, String serialized) {
      String real = this.getMember(type, serialized, this.aliasToFieldMap);
      return real == null ? super.realMember(type, serialized) : real;
   }

   private String getMember(Class type, String name, MemberStore store) {
      for (Class declaringType = type; declaringType != Object.class && declaringType != null; declaringType = declaringType.getSuperclass()) {
         String member = (String)store.get(declaringType, name);
         if (member != null) {
            return member;
         }
      }

      return null;
   }
}
