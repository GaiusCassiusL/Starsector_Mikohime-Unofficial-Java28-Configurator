package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.Map;

class ToAttributedValueConverter$1 implements ReflectionProvider.Visitor {
   ToAttributedValueConverter$1(
      ToAttributedValueConverter this$0, Map var2, Class var3, Class[] var4, Class[] var5, Object[] var6, String[] var7, HierarchicalStreamWriter var8
   ) {
      this.this$0 = this$0;
      this.val$defaultFieldDefinition = var2;
      this.val$sourceType = var3;
      this.val$definingType = var4;
      this.val$fieldType = var5;
      this.val$realValue = var6;
      this.val$tagValue = var7;
      this.val$writer = var8;
   }

   public void visit(String fieldName, Class type, Class definedIn, Object value) {
      if (ToAttributedValueConverter.access$000(this.this$0).shouldSerializeMember(definedIn, fieldName)) {
         String alias = ToAttributedValueConverter.access$000(this.this$0).serializedMember(definedIn, fieldName);
         if (!this.val$defaultFieldDefinition.containsKey(alias)) {
            Class lookupType = this.val$sourceType;
            this.val$defaultFieldDefinition.put(alias, ToAttributedValueConverter.access$100(this.this$0).getField(lookupType, fieldName));
         } else if (!ToAttributedValueConverter.access$200(this.this$0, definedIn, fieldName)) {
            ConversionException exception = new ConversionException("Cannot write attribute twice for object");
            exception.add("alias", alias);
            exception.add("type", this.val$sourceType.getName());
            throw exception;
         }

         ConverterMatcher converter = UseAttributeForEnumMapper.isEnum(type)
            ? ToAttributedValueConverter.access$300(this.this$0).getConverterFromItemType(null, type, null)
            : ToAttributedValueConverter.access$000(this.this$0).getLocalConverter(definedIn, fieldName);
         if (converter == null) {
            converter = ToAttributedValueConverter.access$400(this.this$0).lookupConverterForType(type);
         }

         if (value != null) {
            boolean isValueField = ToAttributedValueConverter.access$500(this.this$0) != null
               && ToAttributedValueConverter.access$200(this.this$0, definedIn, fieldName);
            if (isValueField) {
               this.val$definingType[0] = definedIn;
               this.val$fieldType[0] = type;
               this.val$realValue[0] = value;
               this.val$tagValue[0] = "";
            }

            if (converter instanceof SingleValueConverter) {
               String str = ((SingleValueConverter)converter).toString(value);
               if (isValueField) {
                  this.val$tagValue[0] = str;
               } else if (str != null) {
                  this.val$writer.addAttribute(alias, str);
               }
            } else if (!isValueField) {
               ConversionException exception = new ConversionException("Cannot write element as attribute");
               exception.add("alias", alias);
               exception.add("type", this.val$sourceType.getName());
               throw exception;
            }
         }
      }
   }
}
