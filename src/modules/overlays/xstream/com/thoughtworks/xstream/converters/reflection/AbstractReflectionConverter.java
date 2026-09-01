package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.Caching;
import com.thoughtworks.xstream.core.ReferencingMarshallingContext;
import com.thoughtworks.xstream.core.util.ArrayIterator;
import com.thoughtworks.xstream.core.util.Fields;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.core.util.MemberDictionary;
import com.thoughtworks.xstream.core.util.Primitives;
import com.thoughtworks.xstream.core.util.SerializationMembers;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public abstract class AbstractReflectionConverter implements Converter, Caching {
   protected final ReflectionProvider reflectionProvider;
   protected final Mapper mapper;
   /** @deprecated */
   protected transient SerializationMethodInvoker serializationMethodInvoker;
   protected transient SerializationMembers serializationMembers;
   private transient ReflectionProvider pureJavaReflectionProvider;

   public AbstractReflectionConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
      this.mapper = mapper;
      this.reflectionProvider = reflectionProvider;
      this.serializationMethodInvoker = new SerializationMethodInvoker();
      this.serializationMembers = this.serializationMethodInvoker.serializationMembers;
   }

   protected boolean canAccess(Class type) {
      try {
         this.reflectionProvider.getFieldOrNull(type, "%");
         return true;
      } catch (NoClassDefFoundError var3) {
         return false;
      }
   }

   public void marshal(Object original, HierarchicalStreamWriter writer, MarshallingContext context) {
      Object source = this.serializationMembers.callWriteReplace(original);
      if (source != original && context instanceof ReferencingMarshallingContext) {
         ((ReferencingMarshallingContext)context).replace(original, source);
      }

      if (source.getClass() != original.getClass()) {
         String attributeName = this.mapper.aliasForSystemAttribute("resolves-to");
         if (attributeName != null) {
            writer.addAttribute(attributeName, this.mapper.serializedClass(source.getClass()));
         }

         context.convertAnother(source);
      } else {
         this.doMarshal(source, writer, context);
      }
   }

   protected void doMarshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      List fields = new ArrayList();
      Map defaultFieldDefinition = new HashMap();
      Class sourceType = source.getClass();
      this.reflectionProvider
         .visitSerializableFields(source, new AbstractReflectionConverter$1(this, defaultFieldDefinition, source, sourceType, writer, fields));
      AbstractReflectionConverter.FieldMarshaller fieldMarshaller = new AbstractReflectionConverter$2(this, writer, sourceType, defaultFieldDefinition, context);
      Map hiddenMappers = new HashMap();

      for (AbstractReflectionConverter.FieldInfo info : fields) {
         if (info.value != null) {
            Field defaultField = (Field)defaultFieldDefinition.get(info.fieldName);
            Mapper.ImplicitCollectionMapping mapping = this.mapper
               .getImplicitCollectionDefForFieldName(defaultField.getDeclaringClass() == info.definedIn ? sourceType : info.definedIn, info.fieldName);
            if (mapping != null) {
               Set mappings = (Set)hiddenMappers.get(info.fieldName);
               if (mappings == null) {
                  mappings = new HashSet();
                  mappings.add(mapping);
                  hiddenMappers.put(info.fieldName, mappings);
               } else if (!mappings.add(mapping)) {
                  mapping = null;
               }
            }

            if (mapping != null) {
               if (context instanceof ReferencingMarshallingContext
                  && info.value != Collections.EMPTY_LIST
                  && info.value != Collections.EMPTY_SET
                  && info.value != Collections.EMPTY_MAP) {
                  ReferencingMarshallingContext refContext = (ReferencingMarshallingContext)context;
                  refContext.registerImplicit(info.value);
               }

               boolean isCollection = info.value instanceof Collection;
               boolean isMap = info.value instanceof Map;
               boolean isEntry = isMap && mapping.getKeyFieldName() == null;
               boolean isArray = info.value.getClass().isArray();
               Iterator iter = isArray
                  ? new ArrayIterator(info.value)
                  : (
                     isCollection
                        ? ((Collection)info.value).iterator()
                        : (isEntry ? ((Map)info.value).entrySet().iterator() : ((Map)info.value).values().iterator())
                  );

               while (iter.hasNext()) {
                  Object obj = iter.next();
                  String itemName;
                  Class itemType;
                  if (obj == null) {
                     itemType = Object.class;
                     itemName = this.mapper.serializedClass(null);
                  } else {
                     if (isEntry) {
                        String entryName = mapping.getItemFieldName() != null ? mapping.getItemFieldName() : this.mapper.serializedClass(Entry.class);
                        Entry entry = (Entry)obj;
                        ExtendedHierarchicalStreamWriterHelper.startNode(writer, entryName, entry.getClass());
                        fieldMarshaller.writeItem(entry.getKey());
                        fieldMarshaller.writeItem(entry.getValue());
                        writer.endNode();
                        continue;
                     }

                     if (mapping.getItemFieldName() != null) {
                        itemType = mapping.getItemType();
                        itemName = mapping.getItemFieldName();
                     } else {
                        itemType = obj.getClass();
                        itemName = this.mapper.serializedClass(itemType);
                     }
                  }

                  fieldMarshaller.writeField(info.fieldName, itemName, itemType, info.definedIn, obj);
               }
            } else {
               fieldMarshaller.writeField(info.fieldName, null, info.type, info.definedIn, info.value);
            }
         }
      }
   }

   protected void marshallField(MarshallingContext context, Object newObj, Field field) {
      context.convertAnother(newObj, this.mapper.getLocalConverter(field.getDeclaringClass(), field.getName()));
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Object result = this.instantiateNewInstance(reader, context);
      result = this.doUnmarshal(result, reader, context);
      return this.serializationMembers.callReadResolve(result);
   }

   public Object doUnmarshal(Object result, HierarchicalStreamReader reader, UnmarshallingContext context) {
      Class resultType = result.getClass();
      MemberDictionary seenFields = new MemberDictionary();
      Iterator it = reader.getAttributeNames();

      while (it.hasNext()) {
         String attrAlias = (String)it.next();
         String attrName = this.mapper.realMember(resultType, this.mapper.attributeForAlias(attrAlias));
         Field field = this.reflectionProvider.getFieldOrNull(resultType, attrName);
         if (field != null && this.shouldUnmarshalField(field)) {
            Class classDefiningField = field.getDeclaringClass();
            if (this.mapper.shouldSerializeMember(classDefiningField, attrName)) {
               SingleValueConverter converter = this.mapper.getConverterFromAttribute(classDefiningField, attrName, field.getType());
               Class type = field.getType();
               if (converter != null) {
                  Object value = converter.fromString(reader.getAttribute(attrAlias));
                  if (type.isPrimitive()) {
                     type = Primitives.box(type);
                  }

                  if (value != null && !type.isAssignableFrom(value.getClass())) {
                     ConversionException exception = new ConversionException("Cannot convert type");
                     exception.add("source-type", value.getClass().getName());
                     exception.add("target-type", type.getName());
                     throw exception;
                  }

                  if (!seenFields.add(classDefiningField, attrName)) {
                     throw new AbstractReflectionConverter.DuplicateFieldException(attrName);
                  }

                  this.reflectionProvider.writeField(result, attrName, value, classDefiningField);
               }
            }
         }
      }

      Map implicitCollectionsForCurrentObject = null;

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         String originalNodeName = reader.getNodeName();
         Class explicitDeclaringClass = this.readDeclaringClass(reader);
         Class fieldDeclaringClass = explicitDeclaringClass == null ? resultType : explicitDeclaringClass;
         String fieldName = this.mapper.realMember(fieldDeclaringClass, originalNodeName);
         Mapper.ImplicitCollectionMapping implicitCollectionMapping = this.mapper.getImplicitCollectionDefForFieldName(fieldDeclaringClass, fieldName);
         String implicitFieldName = null;
         Field field = null;
         Class type = null;
         Object value;
         if (implicitCollectionMapping == null) {
            field = this.reflectionProvider.getFieldOrNull(fieldDeclaringClass, fieldName);
            if (field == null) {
               Class itemType = this.mapper.getItemTypeForItemFieldName(fieldDeclaringClass, fieldName);
               if (itemType != null) {
                  String classAttribute = HierarchicalStreams.readClassAttribute(reader, this.mapper);
                  if (classAttribute != null) {
                     type = this.mapper.realClass(classAttribute);
                  } else {
                     type = itemType;
                  }
               } else {
                  try {
                     type = this.mapper.realClass(originalNodeName);
                     implicitFieldName = this.mapper.getFieldNameForItemTypeAndName(fieldDeclaringClass, type, originalNodeName);
                  } catch (CannotResolveClassException var20) {
                  }

                  if (type == null || type != null && implicitFieldName == null) {
                     this.handleUnknownField(explicitDeclaringClass, fieldName, fieldDeclaringClass, originalNodeName);
                     type = null;
                  }
               }

               if (type == null) {
                  value = null;
               } else if (Entry.class.equals(type)) {
                  reader.moveDown();
                  Object key = context.convertAnother(result, HierarchicalStreams.readClassType(reader, this.mapper));
                  reader.moveUp();
                  reader.moveDown();
                  Object v = context.convertAnother(result, HierarchicalStreams.readClassType(reader, this.mapper));
                  reader.moveUp();
                  value = Collections.singletonMap(key, v).entrySet().iterator().next();
               } else {
                  value = context.convertAnother(result, type);
               }
            } else {
               boolean fieldAlreadyChecked = false;
               if (explicitDeclaringClass == null) {
                  while (
                     field != null
                        && !(fieldAlreadyChecked = this.shouldUnmarshalField(field) && this.mapper.shouldSerializeMember(field.getDeclaringClass(), fieldName))
                  ) {
                     field = this.reflectionProvider.getFieldOrNull(field.getDeclaringClass().getSuperclass(), fieldName);
                  }
               }

               if (field != null
                  && (fieldAlreadyChecked || this.shouldUnmarshalField(field) && this.mapper.shouldSerializeMember(field.getDeclaringClass(), fieldName))) {
                  String classAttribute = HierarchicalStreams.readClassAttribute(reader, this.mapper);
                  if (classAttribute != null) {
                     type = this.mapper.realClass(classAttribute);
                  } else {
                     type = this.mapper.defaultImplementationOf(field.getType());
                  }

                  value = this.unmarshallField(context, result, type, field);
                  Class definedType = field.getType();
                  if (!definedType.isPrimitive()) {
                     type = definedType;
                  }
               } else {
                  value = null;
               }
            }
         } else {
            implicitFieldName = implicitCollectionMapping.getFieldName();
            type = implicitCollectionMapping.getItemType();
            if (type == null) {
               String classAttribute = HierarchicalStreams.readClassAttribute(reader, this.mapper);
               type = this.mapper.realClass(classAttribute != null ? classAttribute : originalNodeName);
            }

            value = context.convertAnother(result, type);
         }

         if (value != null && !type.isAssignableFrom(value.getClass())) {
            throw new ConversionException("Cannot convert type " + value.getClass().getName() + " to type " + type.getName());
         }

         if (field != null) {
            this.reflectionProvider.writeField(result, fieldName, value, field.getDeclaringClass());
            if (!seenFields.add(field.getDeclaringClass(), fieldName)) {
               throw new AbstractReflectionConverter.DuplicateFieldException(fieldName);
            }
         } else if (type != null) {
            if (implicitFieldName == null) {
               implicitFieldName = this.mapper
                  .getFieldNameForItemTypeAndName(fieldDeclaringClass, value != null ? value.getClass() : Mapper.Null.class, originalNodeName);
            }

            if (implicitCollectionsForCurrentObject == null) {
               implicitCollectionsForCurrentObject = new HashMap();
            }

            this.writeValueToImplicitCollection(
               value, implicitCollectionsForCurrentObject, result, new AbstractReflectionConverter.FieldLocation(implicitFieldName, fieldDeclaringClass)
            );
         }

         reader.moveUp();
      }

      if (implicitCollectionsForCurrentObject != null) {
         for (Entry entry : implicitCollectionsForCurrentObject.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof AbstractReflectionConverter.ArraysList) {
               Object array = ((AbstractReflectionConverter.ArraysList)value).toPhysicalArray();
               AbstractReflectionConverter.FieldLocation fieldLocation = (AbstractReflectionConverter.FieldLocation)entry.getKey();
               Field field = this.reflectionProvider.getFieldOrNull(fieldLocation.definedIn, fieldLocation.fieldName);
               this.reflectionProvider.writeField(result, fieldLocation.fieldName, array, field != null ? field.getDeclaringClass() : null);
            }
         }
      }

      return result;
   }

   protected Object unmarshallField(UnmarshallingContext context, Object result, Class type, Field field) {
      return context.convertAnother(result, type, this.mapper.getLocalConverter(field.getDeclaringClass(), field.getName()));
   }

   protected boolean shouldUnmarshalTransientFields() {
      return false;
   }

   protected boolean shouldUnmarshalField(Field field) {
      return !Modifier.isTransient(field.getModifiers()) || this.shouldUnmarshalTransientFields();
   }

   private void handleUnknownField(Class classDefiningField, String fieldName, Class resultType, String originalNodeName) {
      if (classDefiningField == null) {
         for (Class cls = resultType; cls != null; cls = cls.getSuperclass()) {
            if (!this.mapper.shouldSerializeMember(cls, originalNodeName)) {
               return;
            }
         }
      }

      throw new AbstractReflectionConverter.UnknownFieldException(resultType.getName(), fieldName);
   }

   private void writeValueToImplicitCollection(Object value, Map implicitCollections, Object result, AbstractReflectionConverter.FieldLocation fieldLocation) {
      Collection collection = (Collection)implicitCollections.get(fieldLocation);
      if (collection == null) {
         Field field = this.reflectionProvider.getFieldOrNull(fieldLocation.definedIn, fieldLocation.fieldName);
         Class physicalFieldType = field != null ? field.getType() : this.reflectionProvider.getFieldType(result, fieldLocation.fieldName, null);
         if (physicalFieldType.isArray()) {
            collection = new AbstractReflectionConverter.ArraysList(physicalFieldType);
         } else {
            Class fieldType = this.mapper.defaultImplementationOf(physicalFieldType);
            if (!Collection.class.isAssignableFrom(fieldType) && !Map.class.isAssignableFrom(fieldType)) {
               ObjectAccessException oaex = new ObjectAccessException("Field is configured for an implicit Collection or Map, but is of an incompatible type");
               oaex.add("field", result.getClass().getName() + "." + fieldLocation.fieldName);
               oaex.add("field-type", fieldType.getName());
               throw oaex;
            }

            if (this.pureJavaReflectionProvider == null) {
               this.pureJavaReflectionProvider = new PureJavaReflectionProvider();
            }

            Object instance = this.pureJavaReflectionProvider.newInstance(fieldType);
            if (instance instanceof Collection) {
               collection = (Collection)instance;
            } else {
               Mapper.ImplicitCollectionMapping implicitCollectionMapping = this.mapper
                  .getImplicitCollectionDefForFieldName(fieldLocation.definedIn, fieldLocation.fieldName);
               collection = new AbstractReflectionConverter.MappingList((Map)instance, implicitCollectionMapping.getKeyFieldName());
            }

            this.reflectionProvider.writeField(result, fieldLocation.fieldName, instance, field != null ? field.getDeclaringClass() : null);
         }

         implicitCollections.put(fieldLocation, collection);
      }

      collection.add(value);
   }

   private Class readDeclaringClass(HierarchicalStreamReader reader) {
      String attributeName = this.mapper.aliasForSystemAttribute("defined-in");
      String definedIn = attributeName == null ? null : reader.getAttribute(attributeName);
      return definedIn == null ? null : this.mapper.realClass(definedIn);
   }

   protected Object instantiateNewInstance(HierarchicalStreamReader reader, UnmarshallingContext context) {
      String attributeName = this.mapper.aliasForSystemAttribute("resolves-to");
      String readResolveValue = attributeName == null ? null : reader.getAttribute(attributeName);
      Object currentObject = context.currentObject();
      if (currentObject != null) {
         return currentObject;
      } else {
         return readResolveValue != null
            ? this.reflectionProvider.newInstance(this.mapper.realClass(readResolveValue))
            : this.reflectionProvider.newInstance(context.getRequiredType());
      }
   }

   public void flushCache() {
      this.serializationMethodInvoker.flushCache();
   }

   protected Object readResolve() {
      this.serializationMethodInvoker = new SerializationMethodInvoker();
      this.serializationMembers = this.serializationMethodInvoker.serializationMembers;
      return this;
   }

   private static class ArraysList extends ArrayList {
      final Class physicalFieldType;

      ArraysList(Class physicalFieldType) {
         this.physicalFieldType = physicalFieldType;
      }

      Object toPhysicalArray() {
         Object[] objects = this.toArray();
         Object array = Array.newInstance(this.physicalFieldType.getComponentType(), objects.length);
         if (this.physicalFieldType.getComponentType().isPrimitive()) {
            for (int i = 0; i < objects.length; i++) {
               Array.set(array, i, Array.get(objects, i));
            }
         } else {
            System.arraycopy(objects, 0, array, 0, objects.length);
         }

         return array;
      }
   }

   public static class DuplicateFieldException extends ConversionException {
      public DuplicateFieldException(String msg) {
         super("Duplicate field " + msg);
         this.add("field", msg);
      }
   }

   private static class FieldInfo extends AbstractReflectionConverter.FieldLocation {
      final Class type;
      final Object value;

      FieldInfo(String fieldName, Class type, Class definedIn, Object value) {
         super(fieldName, definedIn);
         this.type = type;
         this.value = value;
      }
   }

   private static class FieldLocation {
      final String fieldName;
      final Class definedIn;

      FieldLocation(String fieldName, Class definedIn) {
         this.fieldName = fieldName;
         this.definedIn = definedIn;
      }

      public int hashCode() {
         int prime = 7;
         int result = 1;
         result = 7 * result + (this.definedIn == null ? 0 : this.definedIn.getName().hashCode());
         return 7 * result + (this.fieldName == null ? 0 : this.fieldName.hashCode());
      }

      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }

         if (obj == null) {
            return false;
         }

         if (this.getClass() != obj.getClass()) {
            return false;
         }

         AbstractReflectionConverter.FieldLocation other = (AbstractReflectionConverter.FieldLocation)obj;
         if (this.definedIn != other.definedIn) {
            return false;
         }

         if (this.fieldName == null) {
            if (other.fieldName != null) {
               return false;
            }
         } else if (!this.fieldName.equals(other.fieldName)) {
            return false;
         }

         return true;
      }
   }

   private interface FieldMarshaller {
      void writeItem(Object var1);

      void writeField(String var1, String var2, Class var3, Class var4, Object var5);
   }

   private class MappingList extends AbstractList {
      private final Map map;
      private final String keyFieldName;
      private final Map fieldCache = new HashMap();

      public MappingList(Map map, String keyFieldName) {
         this.map = map;
         this.keyFieldName = keyFieldName;
      }

      public boolean add(Object object) {
         if (object == null) {
            boolean containsNull = !this.map.containsKey(null);
            this.map.put(null, null);
            return containsNull;
         }

         Class itemType = object.getClass();
         if (this.keyFieldName != null) {
            Field field = (Field)this.fieldCache.get(itemType);
            if (field == null) {
               field = AbstractReflectionConverter.this.reflectionProvider.getField(itemType, this.keyFieldName);
               this.fieldCache.put(itemType, field);
            }

            if (field != null) {
               Object key = Fields.read(field, object);
               return this.map.put(key, object) == null;
            }
         } else if (object instanceof Entry) {
            Entry entry = (Entry)object;
            return this.map.put(entry.getKey(), entry.getValue()) == null;
         }

         ConversionException exception = new ConversionException("Element  is not defined as entry for implicit map");
         exception.add("map-type", this.map.getClass().getName());
         exception.add("element-type", object.getClass().getName());
         throw exception;
      }

      public Object get(int index) {
         throw new UnsupportedOperationException();
      }

      public int size() {
         return this.map.size();
      }
   }

   public static class UnknownFieldException extends ConversionException {
      public UnknownFieldException(String type, String field) {
         super("No such field " + type + "." + field);
         this.add("field", field);
      }
   }
}
