package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class MapperWrapper implements Mapper {
   private final Mapper wrapped;
   private final Mapper aliasForAttributeMapper;
   private final Mapper aliasForSystemAttributeMapper;
   private final Mapper attributeForAliasMapper;
   private final Mapper defaultImplementationOfMapper;
   private final Mapper getConverterFromAttributeMapper;
   private final Mapper getConverterFromItemTypeMapper;
   private final Mapper getFieldNameForItemTypeAndNameMapper;
   private final Mapper getImplicitCollectionDefForFieldNameMapper;
   private final Mapper getItemTypeForItemFieldNameMapper;
   private final Mapper getLocalConverterMapper;
   private final Mapper isIgnoredElementMapper;
   private final Mapper isImmutableValueTypeMapper;
   private final Mapper isReferenceableMapper;
   private final Mapper realClassMapper;
   private final Mapper realMemberMapper;
   private final Mapper serializedClassMapper;
   private final Mapper serializedMemberMapper;
   private final Mapper shouldSerializeMemberMapper;

   public MapperWrapper(Mapper wrapped) {
      this.wrapped = wrapped;
      if (wrapped instanceof MapperWrapper) {
         MapperWrapper wrapper = (MapperWrapper)wrapped;
         Map wrapperMap = new HashMap();
         wrapperMap.put("aliasForAttribute", wrapper.aliasForAttributeMapper);
         wrapperMap.put("aliasForSystemAttribute", wrapper.aliasForSystemAttributeMapper);
         wrapperMap.put("attributeForAlias", wrapper.attributeForAliasMapper);
         wrapperMap.put("defaultImplementationOf", wrapper.defaultImplementationOfMapper);
         wrapperMap.put("getConverterFromAttribute", wrapper.getConverterFromAttributeMapper);
         wrapperMap.put("getConverterFromItemType", wrapper.getConverterFromItemTypeMapper);
         wrapperMap.put("getFieldNameForItemTypeAndName", wrapper.getFieldNameForItemTypeAndNameMapper);
         wrapperMap.put("getImplicitCollectionDefForFieldName", wrapper.getImplicitCollectionDefForFieldNameMapper);
         wrapperMap.put("getItemTypeForItemFieldName", wrapper.getItemTypeForItemFieldNameMapper);
         wrapperMap.put("getLocalConverter", wrapper.getLocalConverterMapper);
         wrapperMap.put("isIgnoredElement", wrapper.isIgnoredElementMapper);
         wrapperMap.put("isImmutableValueType", wrapper.isImmutableValueTypeMapper);
         wrapperMap.put("isReferenceable", wrapper.isReferenceableMapper);
         wrapperMap.put("realClass", wrapper.realClassMapper);
         wrapperMap.put("realMember", wrapper.realMemberMapper);
         wrapperMap.put("serializedClass", wrapper.serializedClassMapper);
         wrapperMap.put("serializedMember", wrapper.serializedMemberMapper);
         wrapperMap.put("shouldSerializeMember", wrapper.shouldSerializeMemberMapper);
         Method[] methods = wrapped.getClass().getMethods();

         for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getDeclaringClass() != MapperWrapper.class) {
               String name = method.getName();
               if (wrapperMap.containsKey(name)) {
                  wrapperMap.put(name, wrapped);
               }
            }
         }

         this.aliasForAttributeMapper = (Mapper)wrapperMap.get("aliasForAttribute");
         this.aliasForSystemAttributeMapper = (Mapper)wrapperMap.get("aliasForSystemAttribute");
         this.attributeForAliasMapper = (Mapper)wrapperMap.get("attributeForAlias");
         this.defaultImplementationOfMapper = (Mapper)wrapperMap.get("defaultImplementationOf");
         this.getConverterFromAttributeMapper = (Mapper)wrapperMap.get("getConverterFromAttribute");
         this.getConverterFromItemTypeMapper = (Mapper)wrapperMap.get("getConverterFromItemType");
         this.getFieldNameForItemTypeAndNameMapper = (Mapper)wrapperMap.get("getFieldNameForItemTypeAndName");
         this.getImplicitCollectionDefForFieldNameMapper = (Mapper)wrapperMap.get("getImplicitCollectionDefForFieldName");
         this.getItemTypeForItemFieldNameMapper = (Mapper)wrapperMap.get("getItemTypeForItemFieldName");
         this.getLocalConverterMapper = (Mapper)wrapperMap.get("getLocalConverter");
         this.isIgnoredElementMapper = (Mapper)wrapperMap.get("isIgnoredElement");
         this.isImmutableValueTypeMapper = (Mapper)wrapperMap.get("isImmutableValueType");
         this.isReferenceableMapper = (Mapper)wrapperMap.get("isReferenceable");
         this.realClassMapper = (Mapper)wrapperMap.get("realClass");
         this.realMemberMapper = (Mapper)wrapperMap.get("realMember");
         this.serializedClassMapper = (Mapper)wrapperMap.get("serializedClass");
         this.serializedMemberMapper = (Mapper)wrapperMap.get("serializedMember");
         this.shouldSerializeMemberMapper = (Mapper)wrapperMap.get("shouldSerializeMember");
      } else {
         this.aliasForAttributeMapper = wrapped;
         this.aliasForSystemAttributeMapper = wrapped;
         this.attributeForAliasMapper = wrapped;
         this.defaultImplementationOfMapper = wrapped;
         this.getConverterFromAttributeMapper = wrapped;
         this.getConverterFromItemTypeMapper = wrapped;
         this.getFieldNameForItemTypeAndNameMapper = wrapped;
         this.getImplicitCollectionDefForFieldNameMapper = wrapped;
         this.getItemTypeForItemFieldNameMapper = wrapped;
         this.getLocalConverterMapper = wrapped;
         this.isIgnoredElementMapper = wrapped;
         this.isImmutableValueTypeMapper = wrapped;
         this.isReferenceableMapper = wrapped;
         this.realClassMapper = wrapped;
         this.realMemberMapper = wrapped;
         this.serializedClassMapper = wrapped;
         this.serializedMemberMapper = wrapped;
         this.shouldSerializeMemberMapper = wrapped;
      }
   }

   public String serializedClass(Class type) {
      return this.serializedClassMapper.serializedClass(type);
   }

   public Class realClass(String elementName) {
      return this.realClassMapper.realClass(elementName);
   }

   public String serializedMember(Class type, String memberName) {
      return this.serializedMemberMapper.serializedMember(type, memberName);
   }

   public String realMember(Class type, String serialized) {
      return this.realMemberMapper.realMember(type, serialized);
   }

   public boolean isImmutableValueType(Class type) {
      return this.isImmutableValueTypeMapper.isImmutableValueType(type);
   }

   public boolean isReferenceable(Class type) {
      return this.isReferenceableMapper.isReferenceable(type);
   }

   public Class defaultImplementationOf(Class type) {
      return this.defaultImplementationOfMapper.defaultImplementationOf(type);
   }

   public String aliasForAttribute(String attribute) {
      return this.aliasForAttributeMapper.aliasForAttribute(attribute);
   }

   public String attributeForAlias(String alias) {
      return this.attributeForAliasMapper.attributeForAlias(alias);
   }

   public String aliasForSystemAttribute(String attribute) {
      return this.aliasForSystemAttributeMapper.aliasForSystemAttribute(attribute);
   }

   public String getFieldNameForItemTypeAndName(Class definedIn, Class itemType, String itemFieldName) {
      return this.getFieldNameForItemTypeAndNameMapper.getFieldNameForItemTypeAndName(definedIn, itemType, itemFieldName);
   }

   public Class getItemTypeForItemFieldName(Class definedIn, String itemFieldName) {
      return this.getItemTypeForItemFieldNameMapper.getItemTypeForItemFieldName(definedIn, itemFieldName);
   }

   public Mapper.ImplicitCollectionMapping getImplicitCollectionDefForFieldName(Class itemType, String fieldName) {
      return this.getImplicitCollectionDefForFieldNameMapper.getImplicitCollectionDefForFieldName(itemType, fieldName);
   }

   public boolean shouldSerializeMember(Class definedIn, String fieldName) {
      return this.shouldSerializeMemberMapper.shouldSerializeMember(definedIn, fieldName);
   }

   public boolean isIgnoredElement(String name) {
      return this.isIgnoredElementMapper.isIgnoredElement(name);
   }

   /** @deprecated */
   public SingleValueConverter getConverterFromItemType(String fieldName, Class type) {
      return this.getConverterFromItemTypeMapper.getConverterFromItemType(fieldName, type);
   }

   /** @deprecated */
   public SingleValueConverter getConverterFromItemType(Class type) {
      return this.getConverterFromItemTypeMapper.getConverterFromItemType(type);
   }

   /** @deprecated */
   public SingleValueConverter getConverterFromAttribute(String name) {
      return this.getConverterFromAttributeMapper.getConverterFromAttribute(name);
   }

   public Converter getLocalConverter(Class definedIn, String fieldName) {
      return this.getLocalConverterMapper.getLocalConverter(definedIn, fieldName);
   }

   public Mapper lookupMapperOfType(Class type) {
      return type.isAssignableFrom(this.getClass()) ? this : this.wrapped.lookupMapperOfType(type);
   }

   public SingleValueConverter getConverterFromItemType(String fieldName, Class type, Class definedIn) {
      return this.getConverterFromItemTypeMapper.getConverterFromItemType(fieldName, type, definedIn);
   }

   /** @deprecated */
   public String aliasForAttribute(Class definedIn, String fieldName) {
      return this.aliasForAttributeMapper.aliasForAttribute(definedIn, fieldName);
   }

   /** @deprecated */
   public String attributeForAlias(Class definedIn, String alias) {
      return this.attributeForAliasMapper.attributeForAlias(definedIn, alias);
   }

   /** @deprecated */
   public SingleValueConverter getConverterFromAttribute(Class type, String attribute) {
      return this.getConverterFromAttributeMapper.getConverterFromAttribute(type, attribute);
   }

   public SingleValueConverter getConverterFromAttribute(Class definedIn, String attribute, Class type) {
      return this.getConverterFromAttributeMapper.getConverterFromAttribute(definedIn, attribute, type);
   }
}
