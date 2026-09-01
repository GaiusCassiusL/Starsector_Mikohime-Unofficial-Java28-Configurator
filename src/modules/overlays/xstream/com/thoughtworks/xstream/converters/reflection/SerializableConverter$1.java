package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.core.util.CustomObjectOutputStream;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.util.Map;

class SerializableConverter$1 implements CustomObjectOutputStream.StreamCallback {
   SerializableConverter$1(SerializableConverter this$0, HierarchicalStreamWriter var2, MarshallingContext var3, Class[] var4, Object var5, boolean[] var6) {
      this.this$0 = this$0;
      this.val$writer = var2;
      this.val$context = var3;
      this.val$currentType = var4;
      this.val$source = var5;
      this.val$writtenClassWrapper = var6;
   }

   public void writeToStream(Object object) {
      if (object == null) {
         this.val$writer.startNode("null");
         this.val$writer.endNode();
      } else {
         ExtendedHierarchicalStreamWriterHelper.startNode(this.val$writer, this.this$0.mapper.serializedClass(object.getClass()), object.getClass());
         this.val$context.convertAnother(object);
         this.val$writer.endNode();
      }
   }

   public void writeFieldsToStream(Map fields) {
      ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(this.val$currentType[0]);
      this.val$writer.startNode("default");

      for (String name : fields.keySet()) {
         if (this.this$0.mapper.shouldSerializeMember(this.val$currentType[0], name)) {
            ObjectStreamField field = objectStreamClass.getField(name);
            Object value = fields.get(name);
            if (field == null) {
               throw new MissingFieldException(value.getClass().getName(), name);
            }

            if (value != null) {
               ExtendedHierarchicalStreamWriterHelper.startNode(
                  this.val$writer, this.this$0.mapper.serializedMember(this.val$source.getClass(), name), value.getClass()
               );
               if (field.getType() != value.getClass() && !field.getType().isPrimitive()) {
                  String attributeName = this.this$0.mapper.aliasForSystemAttribute("class");
                  if (attributeName != null) {
                     this.val$writer.addAttribute(attributeName, this.this$0.mapper.serializedClass(value.getClass()));
                  }
               }

               this.val$context.convertAnother(value);
               this.val$writer.endNode();
            }
         }
      }

      this.val$writer.endNode();
   }

   public void defaultWriteObject() {
      boolean writtenDefaultFields = false;
      ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(this.val$currentType[0]);
      if (objectStreamClass != null) {
         ObjectStreamField[] fields = objectStreamClass.getFields();

         for (int i = 0; i < fields.length; i++) {
            ObjectStreamField field = fields[i];
            Object value = SerializableConverter.access$000(this.this$0, field, this.val$currentType[0], this.val$source);
            if (value != null) {
               if (!this.val$writtenClassWrapper[0]) {
                  this.val$writer.startNode(this.this$0.mapper.serializedClass(this.val$currentType[0]));
                  this.val$writtenClassWrapper[0] = true;
               }

               if (!writtenDefaultFields) {
                  this.val$writer.startNode("default");
                  writtenDefaultFields = true;
               }

               if (this.this$0.mapper.shouldSerializeMember(this.val$currentType[0], field.getName())) {
                  Class actualType = value.getClass();
                  ExtendedHierarchicalStreamWriterHelper.startNode(
                     this.val$writer, this.this$0.mapper.serializedMember(this.val$source.getClass(), field.getName()), actualType
                  );
                  Class defaultType = this.this$0.mapper.defaultImplementationOf(field.getType());
                  if (!actualType.equals(defaultType)) {
                     String attributeName = this.this$0.mapper.aliasForSystemAttribute("class");
                     if (attributeName != null) {
                        this.val$writer.addAttribute(attributeName, this.this$0.mapper.serializedClass(actualType));
                     }
                  }

                  this.val$context.convertAnother(value);
                  this.val$writer.endNode();
               }
            }
         }

         if (this.val$writtenClassWrapper[0] && !writtenDefaultFields) {
            this.val$writer.startNode("default");
            this.val$writer.endNode();
         } else if (writtenDefaultFields) {
            this.val$writer.endNode();
         }
      }
   }

   public void flush() {
      this.val$writer.flush();
   }

   public void close() {
      throw new UnsupportedOperationException("Objects are not allowed to call ObjectOutputStream.close() from writeObject()");
   }
}
