package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.enums.EnumSingleValueConverter;
import com.thoughtworks.xstream.core.Caching;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class EnumMapper extends MapperWrapper implements Caching {
   private transient AttributeMapper attributeMapper;
   private transient Map<Class, SingleValueConverter> enumConverterMap;

   @Deprecated
   public EnumMapper(Mapper wrapped, ConverterLookup lookup) {
      super(wrapped);
      this.readResolve();
   }

   public EnumMapper(Mapper wrapped) {
      super(wrapped);
      this.readResolve();
   }

   @Override
   public String serializedClass(Class type) {
      if (type == null) {
         return super.serializedClass(type);
      } else if (Enum.class.isAssignableFrom(type) && type.getSuperclass() != Enum.class) {
         return super.serializedClass(type.getSuperclass());
      } else {
         return EnumSet.class.isAssignableFrom(type) ? super.serializedClass(EnumSet.class) : super.serializedClass(type);
      }
   }

   @Override
   public boolean isImmutableValueType(Class type) {
      return Enum.class.isAssignableFrom(type) || super.isImmutableValueType(type);
   }

   @Override
   public boolean isReferenceable(Class type) {
      return type != null && Enum.class.isAssignableFrom(type) ? false : super.isReferenceable(type);
   }

   @Override
   public SingleValueConverter getConverterFromItemType(String fieldName, Class type, Class definedIn) {
      SingleValueConverter converter = this.getLocalConverter(fieldName, type, definedIn);
      return converter == null ? super.getConverterFromItemType(fieldName, type, definedIn) : converter;
   }

   @Override
   public SingleValueConverter getConverterFromAttribute(Class definedIn, String attribute, Class type) {
      SingleValueConverter converter = this.getLocalConverter(attribute, type, definedIn);
      return converter == null ? super.getConverterFromAttribute(definedIn, attribute, type) : converter;
   }

   private SingleValueConverter getLocalConverter(String fieldName, Class type, Class definedIn) {
      if (this.attributeMapper != null
         && Enum.class.isAssignableFrom(type)
         && this.attributeMapper.shouldLookForSingleValueConverter(fieldName, type, definedIn)) {
         synchronized (this.enumConverterMap) {
            SingleValueConverter singleValueConverter = this.enumConverterMap.get(type);
            if (singleValueConverter == null) {
               singleValueConverter = super.getConverterFromItemType(fieldName, type, definedIn);
               if (singleValueConverter == null) {
                  Class<? extends Enum> enumType = type;
                  singleValueConverter = new EnumSingleValueConverter(enumType);
               }

               this.enumConverterMap.put(type, singleValueConverter);
            }

            return singleValueConverter;
         }
      } else {
         return null;
      }
   }

   @Override
   public void flushCache() {
      if (this.enumConverterMap.size() > 0) {
         synchronized (this.enumConverterMap) {
            this.enumConverterMap.clear();
         }
      }
   }

   private Object readResolve() {
      this.enumConverterMap = new HashMap<>();
      this.attributeMapper = (AttributeMapper)this.lookupMapperOfType(AttributeMapper.class);
      return this;
   }
}
