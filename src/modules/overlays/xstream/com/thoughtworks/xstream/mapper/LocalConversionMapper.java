package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.core.util.MemberStore;

public class LocalConversionMapper extends MapperWrapper {
   private final MemberStore localConverters = MemberStore.newInstance();
   private transient AttributeMapper attributeMapper;

   public LocalConversionMapper(Mapper wrapped) {
      super(wrapped);
      this.readResolve();
   }

   public void registerLocalConverter(Class definedIn, String fieldName, Converter converter) {
      this.localConverters.put(definedIn, fieldName, converter);
   }

   public Converter getLocalConverter(Class definedIn, String fieldName) {
      return (Converter)this.localConverters.get(definedIn, fieldName);
   }

   public SingleValueConverter getConverterFromAttribute(Class definedIn, String attribute, Class type) {
      SingleValueConverter converter = this.getLocalSingleValueConverter(definedIn, attribute, type);
      return converter == null ? super.getConverterFromAttribute(definedIn, attribute, type) : converter;
   }

   public SingleValueConverter getConverterFromItemType(String fieldName, Class type, Class definedIn) {
      SingleValueConverter converter = this.getLocalSingleValueConverter(definedIn, fieldName, type);
      return converter == null ? super.getConverterFromItemType(fieldName, type, definedIn) : converter;
   }

   private SingleValueConverter getLocalSingleValueConverter(Class definedIn, String fieldName, Class type) {
      if (this.attributeMapper != null && this.attributeMapper.shouldLookForSingleValueConverter(fieldName, type, definedIn)) {
         Converter converter = this.getLocalConverter(definedIn, fieldName);
         if (converter != null && converter instanceof SingleValueConverter) {
            return (SingleValueConverter)converter;
         }
      }

      return null;
   }

   private Object readResolve() {
      this.attributeMapper = (AttributeMapper)this.lookupMapperOfType(AttributeMapper.class);
      return this;
   }
}
