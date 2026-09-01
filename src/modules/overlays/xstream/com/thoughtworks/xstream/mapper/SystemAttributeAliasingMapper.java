package com.thoughtworks.xstream.mapper;

public class SystemAttributeAliasingMapper extends AbstractAttributeAliasingMapper {
   public SystemAttributeAliasingMapper(Mapper wrapped) {
      super(wrapped);
   }

   public String aliasForSystemAttribute(String attribute) {
      String alias = (String)this.nameToAlias.get(attribute);
      if (alias == null && !this.nameToAlias.containsKey(attribute)) {
         alias = super.aliasForSystemAttribute(attribute);
         if (alias == attribute) {
            alias = super.aliasForAttribute(attribute);
         }
      }

      return alias;
   }
}
