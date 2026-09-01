package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.CustomObjectInputStream;
import com.thoughtworks.xstream.core.util.CustomObjectOutputStream;
import com.thoughtworks.xstream.core.util.Fields;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.IOException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SerializableConverter extends AbstractReflectionConverter {
   private static final String ELEMENT_NULL = "null";
   private static final String ELEMENT_DEFAULT = "default";
   private static final String ELEMENT_UNSERIALIZABLE_PARENTS = "unserializable-parents";
   private static final String ATTRIBUTE_CLASS = "class";
   private static final String ATTRIBUTE_SERIALIZATION = "serialization";
   private static final String ATTRIBUTE_VALUE_CUSTOM = "custom";
   private static final String ELEMENT_FIELDS = "fields";
   private static final String ELEMENT_FIELD = "field";
   private static final String ATTRIBUTE_NAME = "name";
   private final ClassLoaderReference classLoaderReference;

   public SerializableConverter(Mapper mapper, ReflectionProvider reflectionProvider, ClassLoaderReference classLoaderReference) {
      super(mapper, new SerializableConverter.UnserializableParentsReflectionProvider(reflectionProvider));
      this.classLoaderReference = classLoaderReference;
   }

   /** @deprecated */
   public SerializableConverter(Mapper mapper, ReflectionProvider reflectionProvider, ClassLoader classLoader) {
      this(mapper, reflectionProvider, new ClassLoaderReference(classLoader));
   }

   /** @deprecated */
   public SerializableConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
      this(mapper, new SerializableConverter.UnserializableParentsReflectionProvider(reflectionProvider), new ClassLoaderReference(null));
   }

   public boolean canConvert(Class type) {
      return JVM.canCreateDerivedObjectOutputStream() && this.isSerializable(type);
   }

   private boolean isSerializable(Class type) {
      if (type != null
         && Serializable.class.isAssignableFrom(type)
         && !type.isInterface()
         && (this.serializationMembers.supportsReadObject(type, true) || this.serializationMembers.supportsWriteObject(type, true))) {
         Iterator iter = this.hierarchyFor(type).iterator();

         while (iter.hasNext()) {
            if (!Serializable.class.isAssignableFrom((Class<?>)iter.next())) {
               return this.canAccess(type);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public void doMarshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      String attributeName = this.mapper.aliasForSystemAttribute("serialization");
      if (attributeName != null) {
         writer.addAttribute(attributeName, "custom");
      }

      Class[] currentType = new Class[1];
      boolean[] writtenClassWrapper = new boolean[]{false};
      CustomObjectOutputStream.StreamCallback callback = new SerializableConverter$1(this, writer, context, currentType, source, writtenClassWrapper);

      try {
         boolean mustHandleUnserializableParent = false;
         Iterator classHieararchy = this.hierarchyFor(source.getClass()).iterator();

         while (classHieararchy.hasNext()) {
            currentType[0] = (Class)classHieararchy.next();
            if (!Serializable.class.isAssignableFrom(currentType[0])) {
               mustHandleUnserializableParent = true;
            } else {
               if (mustHandleUnserializableParent) {
                  this.marshalUnserializableParent(writer, context, source);
                  mustHandleUnserializableParent = false;
               }

               if (this.serializationMembers.supportsWriteObject(currentType[0], false)) {
                  writtenClassWrapper[0] = true;
                  writer.startNode(this.mapper.serializedClass(currentType[0]));
                  if (currentType[0] != this.mapper.defaultImplementationOf(currentType[0])) {
                     String classAttributeName = this.mapper.aliasForSystemAttribute("class");
                     if (classAttributeName != null) {
                        writer.addAttribute(classAttributeName, currentType[0].getName());
                     }
                  }

                  CustomObjectOutputStream objectOutputStream = CustomObjectOutputStream.getInstance(context, callback);
                  this.serializationMembers.callWriteObject(currentType[0], source, objectOutputStream);
                  objectOutputStream.popCallback();
                  writer.endNode();
               } else if (this.serializationMembers.supportsReadObject(currentType[0], false)) {
                  writtenClassWrapper[0] = true;
                  writer.startNode(this.mapper.serializedClass(currentType[0]));
                  if (currentType[0] != this.mapper.defaultImplementationOf(currentType[0])) {
                     String classAttributeName = this.mapper.aliasForSystemAttribute("class");
                     if (classAttributeName != null) {
                        writer.addAttribute(classAttributeName, currentType[0].getName());
                     }
                  }

                  callback.defaultWriteObject();
                  writer.endNode();
               } else {
                  writtenClassWrapper[0] = false;
                  callback.defaultWriteObject();
                  if (writtenClassWrapper[0]) {
                     writer.endNode();
                  }
               }
            }
         }
      } catch (IOException e) {
         throw new StreamException("Cannot write defaults", e);
      }
   }

   protected void marshalUnserializableParent(HierarchicalStreamWriter writer, MarshallingContext context, Object replacedSource) {
      writer.startNode("unserializable-parents");
      super.doMarshal(replacedSource, writer, context);
      writer.endNode();
   }

   private Object readField(ObjectStreamField field, Class type, Object instance) {
      Field javaField = Fields.find(type, field.getName());
      return Fields.read(javaField, instance);
   }

   protected List hierarchyFor(Class type) {
      List result = new ArrayList();

      while (type != Object.class && type != null) {
         result.add(type);
         type = type.getSuperclass();
      }

      Collections.reverse(result);
      return result;
   }

   public Object doUnmarshal(Object result, HierarchicalStreamReader reader, UnmarshallingContext context) {
      Class[] currentType = new Class[1];
      String attributeName = this.mapper.aliasForSystemAttribute("serialization");
      if (attributeName != null && !"custom".equals(reader.getAttribute(attributeName))) {
         throw new ConversionException("Cannot deserialize object with new readObject()/writeObject() methods");
      }

      CustomObjectInputStream.StreamCallback callback = new SerializableConverter$2(this, reader, context, result, currentType);

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         String nodeName = reader.getNodeName();
         if (nodeName.equals("unserializable-parents")) {
            super.doUnmarshal(result, reader, context);
         } else {
            String classAttribute = HierarchicalStreams.readClassAttribute(reader, this.mapper);
            if (classAttribute == null) {
               currentType[0] = this.mapper.defaultImplementationOf(this.mapper.realClass(nodeName));
            } else {
               currentType[0] = this.mapper.realClass(classAttribute);
            }

            if (this.serializationMembers.supportsReadObject(currentType[0], false)) {
               CustomObjectInputStream objectInputStream = CustomObjectInputStream.getInstance(context, callback, this.classLoaderReference);
               this.serializationMembers.callReadObject(currentType[0], result, objectInputStream);
               objectInputStream.popCallback();
            } else {
               try {
                  callback.defaultReadObject();
               } catch (IOException e) {
                  throw new StreamException("Cannot read defaults", e);
               }
            }
         }

         reader.moveUp();
      }

      return result;
   }

   protected void doMarshalConditionally(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      if (this.isSerializable(source.getClass())) {
         this.doMarshal(source, writer, context);
      } else {
         super.doMarshal(source, writer, context);
      }
   }

   protected Object doUnmarshalConditionally(Object result, HierarchicalStreamReader reader, UnmarshallingContext context) {
      return this.isSerializable(result.getClass()) ? this.doUnmarshal(result, reader, context) : super.doUnmarshal(result, reader, context);
   }

   private static class UnserializableParentsReflectionProvider extends ReflectionProviderWrapper {
      public UnserializableParentsReflectionProvider(ReflectionProvider reflectionProvider) {
         super(reflectionProvider);
      }

      public void visitSerializableFields(Object object, ReflectionProvider.Visitor visitor) {
         this.wrapped.visitSerializableFields(object, new SerializableConverter$UnserializableParentsReflectionProvider$1(this, visitor));
      }
   }
}
