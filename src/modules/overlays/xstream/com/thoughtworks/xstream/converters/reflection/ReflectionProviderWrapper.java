package com.thoughtworks.xstream.converters.reflection;

import java.lang.reflect.Field;

public class ReflectionProviderWrapper implements ReflectionProvider {
   protected final ReflectionProvider wrapped;

   public ReflectionProviderWrapper(ReflectionProvider wrapper) {
      this.wrapped = wrapper;
   }

   /** @deprecated */
   public boolean fieldDefinedInClass(String fieldName, Class type) {
      return this.wrapped.fieldDefinedInClass(fieldName, type);
   }

   public Field getField(Class definedIn, String fieldName) {
      return this.wrapped.getField(definedIn, fieldName);
   }

   public Field getFieldOrNull(Class definedIn, String fieldName) {
      return this.wrapped.getFieldOrNull(definedIn, fieldName);
   }

   public Class getFieldType(Object object, String fieldName, Class definedIn) {
      return this.wrapped.getFieldType(object, fieldName, definedIn);
   }

   public Object newInstance(Class type) {
      return this.wrapped.newInstance(type);
   }

   public void visitSerializableFields(Object object, ReflectionProvider.Visitor visitor) {
      this.wrapped.visitSerializableFields(object, visitor);
   }

   public void writeField(Object object, String fieldName, Object value, Class definedIn) {
      this.wrapped.writeField(object, fieldName, value, definedIn);
   }
}
