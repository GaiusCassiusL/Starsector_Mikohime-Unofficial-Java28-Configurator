package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.core.util.MemberDictionary;
import com.thoughtworks.xstream.core.util.Primitives;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ToAttributedValueConverter implements Converter {
   private static final String STRUCTURE_MARKER = "";
   private final Class type;
   private final Mapper mapper;
   private final Mapper enumMapper;
   private final ReflectionProvider reflectionProvider;
   private final ConverterLookup lookup;
   private final Field valueField;

   public ToAttributedValueConverter(Class type, Mapper mapper, ReflectionProvider reflectionProvider, ConverterLookup lookup) {
      this(type, mapper, reflectionProvider, lookup, null, null);
   }

   public ToAttributedValueConverter(Class type, Mapper mapper, ReflectionProvider reflectionProvider, ConverterLookup lookup, String valueFieldName) {
      this(type, mapper, reflectionProvider, lookup, valueFieldName, null);
   }

   public ToAttributedValueConverter(
      Class type, Mapper mapper, ReflectionProvider reflectionProvider, ConverterLookup lookup, String valueFieldName, Class valueDefinedIn
   ) {
      this.type = type;
      this.mapper = mapper;
      this.reflectionProvider = reflectionProvider;
      this.lookup = lookup;
      if (valueFieldName == null) {
         this.valueField = null;
      } else {
         Field field = null;

         try {
            field = (valueDefinedIn != null ? valueDefinedIn : type).getDeclaredField(valueFieldName);
            if (!field.isAccessible()) {
               field.setAccessible(true);
            }
         } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(e.getMessage() + ": " + valueFieldName);
         }

         this.valueField = field;
      }

      this.enumMapper = JVM.isVersion(5) ? UseAttributeForEnumMapper.createEnumMapper(mapper) : null;
   }

   public boolean canConvert(Class type) {
      return this.type == type;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Class sourceType = source.getClass();
      Map defaultFieldDefinition = new HashMap();
      String[] tagValue = new String[1];
      Object[] realValue = new Object[1];
      Class[] fieldType = new Class[1];
      Class[] definingType = new Class[1];
      this.reflectionProvider
         .visitSerializableFields(
            source, new ToAttributedValueConverter$1(this, defaultFieldDefinition, sourceType, definingType, fieldType, realValue, tagValue, writer)
         );
      if (tagValue[0] != null) {
         Class actualType = realValue[0].getClass();
         Class defaultType = this.mapper.defaultImplementationOf(fieldType[0]);
         if (!actualType.equals(defaultType)) {
            String serializedClassName = this.mapper.serializedClass(actualType);
            if (!serializedClassName.equals(this.mapper.serializedClass(defaultType))) {
               String attributeName = this.mapper.aliasForSystemAttribute("class");
               if (attributeName != null) {
                  writer.addAttribute(attributeName, serializedClassName);
               }
            }
         }

         if (tagValue[0] == "") {
            context.convertAnother(realValue[0]);
         } else {
            writer.setValue(tagValue[0]);
         }
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Object result = this.reflectionProvider.newInstance(context.getRequiredType());
      Class resultType = result.getClass();
      MemberDictionary seenFields = new MemberDictionary();
      Iterator it = reader.getAttributeNames();
      Set systemAttributes = new HashSet();
      systemAttributes.add(this.mapper.aliasForSystemAttribute("class"));

      while (it.hasNext()) {
         String attrName = (String)it.next();
         if (!systemAttributes.contains(attrName)) {
            String fieldName = this.mapper.realMember(resultType, attrName);
            Field field = this.reflectionProvider.getFieldOrNull(resultType, fieldName);
            if (field != null && !Modifier.isTransient(field.getModifiers())) {
               Class type = field.getType();
               Class declaringClass = field.getDeclaringClass();
               ConverterMatcher converter = UseAttributeForEnumMapper.isEnum(type)
                  ? this.enumMapper.getConverterFromItemType(null, type, null)
                  : this.mapper.getLocalConverter(declaringClass, fieldName);
               if (converter == null) {
                  converter = this.lookup.lookupConverterForType(type);
               }

               if (!(converter instanceof SingleValueConverter)) {
                  ConversionException exception = new ConversionException("Cannot read field as a single value for object");
                  exception.add("field", fieldName);
                  exception.add("type", resultType.getName());
                  throw exception;
               }

               if (converter != null) {
                  Object value = ((SingleValueConverter)converter).fromString(reader.getAttribute(attrName));
                  if (type.isPrimitive()) {
                     type = Primitives.box(type);
                  }

                  if (value != null && !type.isAssignableFrom(value.getClass())) {
                     ConversionException exception = new ConversionException("Cannot assign object to type");
                     exception.add("object type", value.getClass().getName());
                     exception.add("target type", type.getName());
                     throw exception;
                  }

                  this.reflectionProvider.writeField(result, fieldName, value, declaringClass);
                  if (!seenFields.add(declaringClass, fieldName)) {
                     throw new AbstractReflectionConverter.DuplicateFieldException(fieldName + " [" + declaringClass.getName() + "]");
                  }
               }
            }
         }
      }

      if (this.valueField != null) {
         Class classDefiningField = this.valueField.getDeclaringClass();
         String fieldName = this.valueField.getName();
         Field field = fieldName == null ? null : this.reflectionProvider.getField(classDefiningField, fieldName);
         if (fieldName == null || field == null) {
            ConversionException exception = new ConversionException("Cannot assign value to field of type");
            exception.add("element", reader.getNodeName());
            exception.add("field", fieldName);
            exception.add("target type", context.getRequiredType().getName());
            throw exception;
         }

         String classAttribute = HierarchicalStreams.readClassAttribute(reader, this.mapper);
         Class type;
         if (classAttribute != null) {
            type = this.mapper.realClass(classAttribute);
         } else {
            type = this.mapper.defaultImplementationOf(this.reflectionProvider.getFieldType(result, fieldName, classDefiningField));
         }

         Object value = context.convertAnother(result, type, this.mapper.getLocalConverter(field.getDeclaringClass(), field.getName()));
         Class definedType = this.reflectionProvider.getFieldType(result, fieldName, classDefiningField);
         if (!definedType.isPrimitive()) {
            type = definedType;
         }

         if (value != null && !type.isAssignableFrom(value.getClass())) {
            ConversionException exception = new ConversionException("Cannot assign object to type");
            exception.add("object type", value.getClass().getName());
            exception.add("target type", type.getName());
            throw exception;
         }

         this.reflectionProvider.writeField(result, fieldName, value, classDefiningField);
         if (!seenFields.add(classDefiningField, fieldName)) {
            throw new AbstractReflectionConverter.DuplicateFieldException(fieldName + " [" + classDefiningField.getName() + "]");
         }
      }

      return result;
   }

   private boolean fieldIsEqual(Class definedIn, String name) {
      return this.valueField.getName().equals(name) && this.valueField.getDeclaringClass().getName().equals(definedIn.getName());
   }
}
