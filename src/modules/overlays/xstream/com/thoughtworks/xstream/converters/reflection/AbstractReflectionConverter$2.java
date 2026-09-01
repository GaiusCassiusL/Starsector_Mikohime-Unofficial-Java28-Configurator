package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.lang.reflect.Field;
import java.util.Map;

class AbstractReflectionConverter$2 implements AbstractReflectionConverter.FieldMarshaller {
   AbstractReflectionConverter$2(AbstractReflectionConverter this$0, HierarchicalStreamWriter var2, Class var3, Map var4, MarshallingContext var5) {
      this.this$0 = this$0;
      this.val$writer = var2;
      this.val$sourceType = var3;
      this.val$defaultFieldDefinition = var4;
      this.val$context = var5;
   }

   public void writeField(String fieldName, String aliasName, Class fieldType, Class definedIn, Object newObj) {
      Class actualType = newObj != null ? newObj.getClass() : fieldType;
      ExtendedHierarchicalStreamWriterHelper.startNode(
         this.val$writer, aliasName != null ? aliasName : this.this$0.mapper.serializedMember(this.val$sourceType, fieldName), actualType
      );
      if (newObj != null) {
         Class defaultType = this.this$0.mapper.defaultImplementationOf(fieldType);
         if (!actualType.equals(defaultType)) {
            String serializedClassName = this.this$0.mapper.serializedClass(actualType);
            if (!serializedClassName.equals(this.this$0.mapper.serializedClass(defaultType))) {
               String attributeName = this.this$0.mapper.aliasForSystemAttribute("class");
               if (attributeName != null) {
                  this.val$writer.addAttribute(attributeName, serializedClassName);
               }
            }
         }

         Field defaultField = (Field)this.val$defaultFieldDefinition.get(fieldName);
         if (defaultField.getDeclaringClass() != definedIn) {
            String attributeName = this.this$0.mapper.aliasForSystemAttribute("defined-in");
            if (attributeName != null) {
               this.val$writer.addAttribute(attributeName, this.this$0.mapper.serializedClass(definedIn));
            }
         }

         Field field = this.this$0.reflectionProvider.getField(definedIn, fieldName);
         this.this$0.marshallField(this.val$context, newObj, field);
      }

      this.val$writer.endNode();
   }

   public void writeItem(Object item) {
      if (item == null) {
         String name = this.this$0.mapper.serializedClass(null);
         ExtendedHierarchicalStreamWriterHelper.startNode(
            this.val$writer,
            name,
            AbstractReflectionConverter.class$com$thoughtworks$xstream$mapper$Mapper$Null == null
               ? (
                  AbstractReflectionConverter.class$com$thoughtworks$xstream$mapper$Mapper$Null = AbstractReflectionConverter.class$(
                     "com.thoughtworks.xstream.mapper.Mapper$Null"
                  )
               )
               : AbstractReflectionConverter.class$com$thoughtworks$xstream$mapper$Mapper$Null
         );
         this.val$writer.endNode();
      } else {
         String name = this.this$0.mapper.serializedClass(item.getClass());
         ExtendedHierarchicalStreamWriterHelper.startNode(this.val$writer, name, item.getClass());
         this.val$context.convertAnother(item);
         this.val$writer.endNode();
      }
   }
}
