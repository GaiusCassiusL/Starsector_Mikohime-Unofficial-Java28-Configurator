package com.thoughtworks.xstream.converters.javabean;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

class JavaBeanConverter$1 implements JavaBeanProvider.Visitor {
   JavaBeanConverter$1(JavaBeanConverter this$0, Object var2, HierarchicalStreamWriter var3, String var4, MarshallingContext var5) {
      this.this$0 = this$0;
      this.val$source = var2;
      this.val$writer = var3;
      this.val$classAttributeName = var4;
      this.val$context = var5;
   }

   public boolean shouldVisit(String name, Class definedIn) {
      return this.this$0.mapper.shouldSerializeMember(definedIn, name);
   }

   public void visit(String propertyName, Class fieldType, Class definedIn, Object newObj) {
      if (newObj != null) {
         this.writeField(propertyName, fieldType, newObj);
      } else {
         this.writeNullField(propertyName);
      }
   }

   private void writeField(String propertyName, Class fieldType, Object newObj) {
      Class actualType = newObj.getClass();
      Class defaultType = this.this$0.mapper.defaultImplementationOf(fieldType);
      String serializedMember = this.this$0.mapper.serializedMember(this.val$source.getClass(), propertyName);
      ExtendedHierarchicalStreamWriterHelper.startNode(this.val$writer, serializedMember, actualType);
      if (!actualType.equals(defaultType) && this.val$classAttributeName != null) {
         this.val$writer.addAttribute(this.val$classAttributeName, this.this$0.mapper.serializedClass(actualType));
      }

      this.val$context.convertAnother(newObj);
      this.val$writer.endNode();
   }

   private void writeNullField(String propertyName) {
      String serializedMember = this.this$0.mapper.serializedMember(this.val$source.getClass(), propertyName);
      ExtendedHierarchicalStreamWriterHelper.startNode(
         this.val$writer,
         serializedMember,
         JavaBeanConverter.class$com$thoughtworks$xstream$mapper$Mapper$Null == null
            ? (JavaBeanConverter.class$com$thoughtworks$xstream$mapper$Mapper$Null = JavaBeanConverter.class$("com.thoughtworks.xstream.mapper.Mapper$Null"))
            : JavaBeanConverter.class$com$thoughtworks$xstream$mapper$Mapper$Null
      );
      this.val$writer
         .addAttribute(
            this.val$classAttributeName,
            this.this$0
               .mapper
               .serializedClass(
                  JavaBeanConverter.class$com$thoughtworks$xstream$mapper$Mapper$Null == null
                     ? (
                        JavaBeanConverter.class$com$thoughtworks$xstream$mapper$Mapper$Null = JavaBeanConverter.class$(
                           "com.thoughtworks.xstream.mapper.Mapper$Null"
                        )
                     )
                     : JavaBeanConverter.class$com$thoughtworks$xstream$mapper$Mapper$Null
               )
         );
      this.val$writer.endNode();
   }
}
