package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AbstractReflectionConverter$1 implements ReflectionProvider.Visitor {
   final Set writtenAttributes;

   AbstractReflectionConverter$1(AbstractReflectionConverter this$0, Map var2, Object var3, Class var4, HierarchicalStreamWriter var5, List var6) {
      this.this$0 = this$0;
      this.val$defaultFieldDefinition = var2;
      this.val$source = var3;
      this.val$sourceType = var4;
      this.val$writer = var5;
      this.val$fields = var6;
      this.writtenAttributes = new HashSet();
   }

   public void visit(String fieldName, Class type, Class definedIn, Object value) {
      if (this.this$0.mapper.shouldSerializeMember(definedIn, fieldName)) {
         if (!this.val$defaultFieldDefinition.containsKey(fieldName)) {
            Class lookupType = this.val$source.getClass();
            if (definedIn != this.val$sourceType && !this.this$0.mapper.shouldSerializeMember(lookupType, fieldName)) {
               lookupType = definedIn;
            }

            this.val$defaultFieldDefinition.put(fieldName, this.this$0.reflectionProvider.getField(lookupType, fieldName));
         }

         SingleValueConverter converter = this.this$0.mapper.getConverterFromItemType(fieldName, type, definedIn);
         if (converter != null) {
            String attribute = this.this$0.mapper.aliasForAttribute(this.this$0.mapper.serializedMember(definedIn, fieldName));
            if (value != null) {
               if (this.writtenAttributes.contains(fieldName)) {
                  ConversionException exception = new ConversionException("Cannot write field as attribute for object, attribute name already in use");
                  exception.add("field-name", fieldName);
                  exception.add("object-type", this.val$sourceType.getName());
                  throw exception;
               }

               String str = converter.toString(value);
               if (str != null) {
                  this.val$writer.addAttribute(attribute, str);
               }
            }

            this.writtenAttributes.add(fieldName);
         } else {
            this.val$fields.add(new AbstractReflectionConverter.FieldInfo(fieldName, type, definedIn, value));
         }
      }
   }
}
