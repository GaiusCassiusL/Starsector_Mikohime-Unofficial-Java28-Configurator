package com.thoughtworks.xstream.converters.javabean;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.MissingFieldException;
import com.thoughtworks.xstream.core.util.MemberDictionary;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class JavaBeanConverter implements Converter {
   protected final Mapper mapper;
   protected final JavaBeanProvider beanProvider;
   private final Class type;
   /** @deprecated */
   private String classAttributeIdentifier;

   public JavaBeanConverter(Mapper mapper) {
      this(mapper, (Class)null);
   }

   public JavaBeanConverter(Mapper mapper, Class type) {
      this(mapper, new BeanProvider(), type);
   }

   public JavaBeanConverter(Mapper mapper, JavaBeanProvider beanProvider) {
      this(mapper, beanProvider, null);
   }

   public JavaBeanConverter(Mapper mapper, JavaBeanProvider beanProvider, Class type) {
      this.mapper = mapper;
      this.beanProvider = beanProvider;
      this.type = type;
   }

   /** @deprecated */
   public JavaBeanConverter(Mapper mapper, String classAttributeIdentifier) {
      this(mapper, new BeanProvider());
      this.classAttributeIdentifier = classAttributeIdentifier;
   }

   public boolean canConvert(Class type) {
      return (this.type == null || this.type == type) && this.beanProvider.canInstantiate(type);
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      String classAttributeName = this.mapper.aliasForSystemAttribute("class");
      this.beanProvider.visitSerializableProperties(source, new JavaBeanConverter$1(this, source, writer, classAttributeName, context));
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Object result = this.instantiateNewInstance(context);
      MemberDictionary seenProperties = new MemberDictionary();
      Class resultType = result.getClass();

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         String propertyName = this.mapper.realMember(resultType, reader.getNodeName());
         if (this.mapper.shouldSerializeMember(resultType, propertyName)) {
            boolean propertyExistsInClass = this.beanProvider.propertyDefinedInClass(propertyName, resultType);
            if (propertyExistsInClass) {
               Class type = this.determineType(reader, result, propertyName);
               Object value = context.convertAnother(result, type);
               this.beanProvider.writeProperty(result, propertyName, value);
               if (!seenProperties.add(resultType, propertyName)) {
                  throw new JavaBeanConverter.DuplicatePropertyException(propertyName);
               }
            } else if (!this.mapper.isIgnoredElement(propertyName)) {
               throw new MissingFieldException(resultType.getName(), propertyName);
            }
         }

         reader.moveUp();
      }

      return result;
   }

   private Object instantiateNewInstance(UnmarshallingContext context) {
      Object result = context.currentObject();
      if (result == null) {
         result = this.beanProvider.newInstance(context.getRequiredType());
      }

      return result;
   }

   private Class determineType(HierarchicalStreamReader reader, Object result, String fieldName) {
      String classAttributeName = this.classAttributeIdentifier != null ? this.classAttributeIdentifier : this.mapper.aliasForSystemAttribute("class");
      String classAttribute = classAttributeName == null ? null : reader.getAttribute(classAttributeName);
      return classAttribute != null
         ? this.mapper.realClass(classAttribute)
         : this.mapper.defaultImplementationOf(this.beanProvider.getPropertyType(result, fieldName));
   }

   /** @deprecated */
   public static class DuplicateFieldException extends ConversionException {
      public DuplicateFieldException(String msg) {
         super(msg);
      }
   }

   public static class DuplicatePropertyException extends ConversionException {
      public DuplicatePropertyException(String msg) {
         super("Duplicate property " + msg);
         this.add("property", msg);
      }
   }
}
