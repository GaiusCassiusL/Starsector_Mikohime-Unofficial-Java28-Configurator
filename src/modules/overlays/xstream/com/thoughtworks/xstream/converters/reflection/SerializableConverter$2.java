package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.CustomObjectInputStream;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import java.io.ObjectInputValidation;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.util.HashMap;
import java.util.Map;

class SerializableConverter$2 implements CustomObjectInputStream.StreamCallback {
   SerializableConverter$2(SerializableConverter this$0, HierarchicalStreamReader var2, UnmarshallingContext var3, Object var4, Class[] var5) {
      this.this$0 = this$0;
      this.val$reader = var2;
      this.val$context = var3;
      this.val$result = var4;
      this.val$currentType = var5;
   }

   public Object readFromStream() {
      this.val$reader.moveDown();
      Class type = HierarchicalStreams.readClassType(this.val$reader, this.this$0.mapper);
      Object value = this.val$context.convertAnother(this.val$result, type);
      this.val$reader.moveUp();
      return value;
   }

   public Map readFieldsFromStream() {
      Map fields = new HashMap();
      this.val$reader.moveDown();
      if (this.val$reader.getNodeName().equals("fields")) {
         while (this.val$reader.hasMoreChildren()) {
            this.val$reader.moveDown();
            if (!this.val$reader.getNodeName().equals("field")) {
               throw new ConversionException("Expected <field/> element inside <field/>");
            }

            String name = this.val$reader.getAttribute("name");
            Class type = this.this$0.mapper.realClass(this.val$reader.getAttribute("class"));
            Object value = this.val$context.convertAnother(this.val$result, type);
            fields.put(name, value);
            this.val$reader.moveUp();
         }
      } else {
         if (!this.val$reader.getNodeName().equals("default")) {
            throw new ConversionException("Expected <fields/> or <default/> element when calling ObjectInputStream.readFields()");
         }

         ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(this.val$currentType[0]);

         while (this.val$reader.hasMoreChildren()) {
            this.val$reader.moveDown();
            String name = this.this$0.mapper.realMember(this.val$currentType[0], this.val$reader.getNodeName());
            if (this.this$0.mapper.shouldSerializeMember(this.val$currentType[0], name)) {
               String classAttribute = HierarchicalStreams.readClassAttribute(this.val$reader, this.this$0.mapper);
               Class type;
               if (classAttribute != null) {
                  type = this.this$0.mapper.realClass(classAttribute);
               } else {
                  ObjectStreamField field = objectStreamClass.getField(name);
                  if (field == null) {
                     throw new MissingFieldException(this.val$currentType[0].getName(), name);
                  }

                  type = field.getType();
               }

               Object value = this.val$context.convertAnother(this.val$result, type);
               fields.put(name, value);
            }

            this.val$reader.moveUp();
         }
      }

      this.val$reader.moveUp();
      return fields;
   }

   public void defaultReadObject() {
      if (this.this$0.serializationMembers.hasSerializablePersistentFields(this.val$currentType[0])) {
         this.readFieldsFromStream();
      } else if (this.val$reader.hasMoreChildren()) {
         this.val$reader.moveDown();
         if (!this.val$reader.getNodeName().equals("default")) {
            throw new ConversionException("Expected <default/> element in readObject() stream");
         }

         for (; this.val$reader.hasMoreChildren(); this.val$reader.moveUp()) {
            this.val$reader.moveDown();
            String fieldName = this.this$0.mapper.realMember(this.val$currentType[0], this.val$reader.getNodeName());
            if (this.this$0.mapper.shouldSerializeMember(this.val$currentType[0], fieldName)) {
               String classAttribute = HierarchicalStreams.readClassAttribute(this.val$reader, this.this$0.mapper);
               Class type;
               if (classAttribute != null) {
                  type = this.this$0.mapper.realClass(classAttribute);
               } else {
                  type = this.this$0
                     .mapper
                     .defaultImplementationOf(this.this$0.reflectionProvider.getFieldType(this.val$result, fieldName, this.val$currentType[0]));
               }

               Object value = this.val$context.convertAnother(this.val$result, type);
               this.this$0.reflectionProvider.writeField(this.val$result, fieldName, value, this.val$currentType[0]);
            }
         }

         this.val$reader.moveUp();
      }
   }

   public void registerValidation(ObjectInputValidation validation, int priority) {
      this.val$context.addCompletionCallback(new SerializableConverter$2$1(this, validation), priority);
   }

   public void close() {
      throw new UnsupportedOperationException("Objects are not allowed to call ObjectInputStream.close() from readObject()");
   }
}
