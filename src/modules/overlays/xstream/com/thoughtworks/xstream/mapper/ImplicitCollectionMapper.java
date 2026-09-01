package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.InitializationException;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.util.Primitives;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ImplicitCollectionMapper extends MapperWrapper {
   private ReflectionProvider reflectionProvider;
   private final Map classNameToMapper = new HashMap();

   public ImplicitCollectionMapper(Mapper wrapped, ReflectionProvider reflectionProvider) {
      super(wrapped);
      this.reflectionProvider = reflectionProvider;
   }

   private ImplicitCollectionMapper.ImplicitCollectionMapperForClass getMapper(Class declaredFor, String fieldName) {
      Class definedIn = declaredFor;
      Field field = fieldName != null ? this.reflectionProvider.getFieldOrNull(definedIn, fieldName) : null;
      Class inheritanceStop = field != null ? field.getDeclaringClass() : null;

      while (definedIn != null) {
         ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = (ImplicitCollectionMapper.ImplicitCollectionMapperForClass)this.classNameToMapper
            .get(definedIn);
         if (mapper != null) {
            return mapper;
         }

         if (definedIn == inheritanceStop) {
            break;
         }

         definedIn = definedIn.getSuperclass();
      }

      return null;
   }

   private ImplicitCollectionMapper.ImplicitCollectionMapperForClass getOrCreateMapper(Class definedIn) {
      ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = (ImplicitCollectionMapper.ImplicitCollectionMapperForClass)this.classNameToMapper
         .get(definedIn);
      if (mapper == null) {
         mapper = new ImplicitCollectionMapper.ImplicitCollectionMapperForClass(definedIn);
         this.classNameToMapper.put(definedIn, mapper);
      }

      return mapper;
   }

   public String getFieldNameForItemTypeAndName(Class definedIn, Class itemType, String itemFieldName) {
      ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = this.getMapper(definedIn, null);
      return mapper != null ? mapper.getFieldNameForItemTypeAndName(itemType, itemFieldName) : null;
   }

   public Class getItemTypeForItemFieldName(Class definedIn, String itemFieldName) {
      ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = this.getMapper(definedIn, null);
      return mapper != null ? mapper.getItemTypeForItemFieldName(itemFieldName) : null;
   }

   public Mapper.ImplicitCollectionMapping getImplicitCollectionDefForFieldName(Class itemType, String fieldName) {
      ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = this.getMapper(itemType, fieldName);
      return mapper != null ? mapper.getImplicitCollectionDefForFieldName(fieldName) : null;
   }

   public void add(Class definedIn, String fieldName, Class itemType) {
      this.add(definedIn, fieldName, null, itemType);
   }

   public void add(Class definedIn, String fieldName, String itemFieldName, Class itemType) {
      this.add(definedIn, fieldName, itemFieldName, itemType, null);
   }

   public void add(Class definedIn, String fieldName, String itemFieldName, Class itemType, String keyFieldName) {
      Field field = null;
      if (definedIn != null) {
         Class declaredIn = definedIn;

         while (declaredIn != Object.class) {
            try {
               field = declaredIn.getDeclaredField(fieldName);
               if (!Modifier.isStatic(field.getModifiers())) {
                  break;
               }

               field = null;
            } catch (SecurityException e) {
               throw new InitializationException("Access denied for field with implicit collection", e);
            } catch (NoSuchFieldException e) {
               declaredIn = declaredIn.getSuperclass();
            }
         }
      }

      if (field == null) {
         throw new InitializationException("No field \"" + fieldName + "\" for implicit collection");
      }

      if (Map.class.isAssignableFrom(field.getType())) {
         if (itemFieldName == null && keyFieldName == null) {
            itemType = Entry.class;
         }
      } else if (!Collection.class.isAssignableFrom(field.getType())) {
         Class fieldType = field.getType();
         if (!fieldType.isArray()) {
            throw new InitializationException("Field \"" + fieldName + "\" declares no collection or array");
         }

         Class componentType = fieldType.getComponentType();
         componentType = componentType.isPrimitive() ? Primitives.box(componentType) : componentType;
         if (itemType == null) {
            itemType = componentType;
         } else {
            itemType = itemType.isPrimitive() ? Primitives.box(itemType) : itemType;
            if (!componentType.isAssignableFrom(itemType)) {
               throw new InitializationException(
                  "Field \"" + fieldName + "\" declares an array, but the array type is not compatible with " + itemType.getName()
               );
            }
         }
      }

      ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = this.getOrCreateMapper(definedIn);
      mapper.add(new ImplicitCollectionMapper.ImplicitCollectionMappingImpl(fieldName, itemType, itemFieldName, keyFieldName));
   }

   private class ImplicitCollectionMapperForClass {
      private Class definedIn;
      private Map namedItemTypeToDef = new HashMap();
      private Map itemFieldNameToDef = new HashMap();
      private Map fieldNameToDef = new HashMap();

      ImplicitCollectionMapperForClass(Class definedIn) {
         this.definedIn = definedIn;
      }

      public String getFieldNameForItemTypeAndName(Class itemType, String itemFieldName) {
         ImplicitCollectionMapper.ImplicitCollectionMappingImpl unnamed = null;

         for (ImplicitCollectionMapper.NamedItemType itemTypeForFieldName : this.namedItemTypeToDef.keySet()) {
            ImplicitCollectionMapper.ImplicitCollectionMappingImpl def = (ImplicitCollectionMapper.ImplicitCollectionMappingImpl)this.namedItemTypeToDef
               .get(itemTypeForFieldName);
            if (itemType == Mapper.Null.class) {
               unnamed = def;
               break;
            }

            if (itemTypeForFieldName.itemType.isAssignableFrom(itemType)) {
               if (def.getItemFieldName() != null) {
                  if (def.getItemFieldName().equals(itemFieldName)) {
                     return def.getFieldName();
                  }
               } else if (unnamed == null
                  || unnamed.getItemType() == null
                  || def.getItemType() != null && unnamed.getItemType().isAssignableFrom(def.getItemType())) {
                  unnamed = def;
               }
            }
         }

         if (unnamed != null) {
            return unnamed.getFieldName();
         }

         ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = ImplicitCollectionMapper.this.getMapper(this.definedIn.getSuperclass(), null);
         return mapper != null ? mapper.getFieldNameForItemTypeAndName(itemType, itemFieldName) : null;
      }

      public Class getItemTypeForItemFieldName(String itemFieldName) {
         ImplicitCollectionMapper.ImplicitCollectionMappingImpl def = this.getImplicitCollectionDefByItemFieldName(itemFieldName);
         if (def != null) {
            return def.getItemType();
         }

         ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = ImplicitCollectionMapper.this.getMapper(this.definedIn.getSuperclass(), null);
         return mapper != null ? mapper.getItemTypeForItemFieldName(itemFieldName) : null;
      }

      private ImplicitCollectionMapper.ImplicitCollectionMappingImpl getImplicitCollectionDefByItemFieldName(String itemFieldName) {
         if (itemFieldName == null) {
            return null;
         }

         ImplicitCollectionMapper.ImplicitCollectionMappingImpl mapping = (ImplicitCollectionMapper.ImplicitCollectionMappingImpl)this.itemFieldNameToDef
            .get(itemFieldName);
         if (mapping != null) {
            return mapping;
         }

         ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = ImplicitCollectionMapper.this.getMapper(this.definedIn.getSuperclass(), null);
         return mapper != null ? mapper.getImplicitCollectionDefByItemFieldName(itemFieldName) : null;
      }

      public Mapper.ImplicitCollectionMapping getImplicitCollectionDefForFieldName(String fieldName) {
         Mapper.ImplicitCollectionMapping mapping = (Mapper.ImplicitCollectionMapping)this.fieldNameToDef.get(fieldName);
         if (mapping != null) {
            return mapping;
         }

         ImplicitCollectionMapper.ImplicitCollectionMapperForClass mapper = ImplicitCollectionMapper.this.getMapper(this.definedIn.getSuperclass(), null);
         return mapper != null ? mapper.getImplicitCollectionDefForFieldName(fieldName) : null;
      }

      public void add(ImplicitCollectionMapper.ImplicitCollectionMappingImpl def) {
         this.fieldNameToDef.put(def.getFieldName(), def);
         this.namedItemTypeToDef.put(def.createNamedItemType(), def);
         if (def.getItemFieldName() != null) {
            this.itemFieldNameToDef.put(def.getItemFieldName(), def);
         }
      }
   }

   private static class ImplicitCollectionMappingImpl implements Mapper.ImplicitCollectionMapping {
      private final String fieldName;
      private final String itemFieldName;
      private final Class itemType;
      private final String keyFieldName;

      ImplicitCollectionMappingImpl(String fieldName, Class itemType, String itemFieldName, String keyFieldName) {
         this.fieldName = fieldName;
         this.itemFieldName = itemFieldName;
         this.itemType = itemType;
         this.keyFieldName = keyFieldName;
      }

      public ImplicitCollectionMapper.NamedItemType createNamedItemType() {
         return new ImplicitCollectionMapper.NamedItemType(this.itemType, this.itemFieldName);
      }

      public String getFieldName() {
         return this.fieldName;
      }

      public String getItemFieldName() {
         return this.itemFieldName;
      }

      public Class getItemType() {
         return this.itemType;
      }

      public String getKeyFieldName() {
         return this.keyFieldName;
      }
   }

   private static class NamedItemType {
      Class itemType;
      String itemFieldName;

      NamedItemType(Class itemType, String itemFieldName) {
         this.itemType = itemType == null ? Object.class : itemType;
         this.itemFieldName = itemFieldName;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof ImplicitCollectionMapper.NamedItemType)) {
            return false;
         }

         ImplicitCollectionMapper.NamedItemType b = (ImplicitCollectionMapper.NamedItemType)obj;
         return this.itemType.equals(b.itemType) && isEquals(this.itemFieldName, b.itemFieldName);
      }

      private static boolean isEquals(Object a, Object b) {
         return a == null ? b == null : a.equals(b);
      }

      public int hashCode() {
         int hash = this.itemType.hashCode() << 7;
         if (this.itemFieldName != null) {
            hash += this.itemFieldName.hashCode();
         }

         return hash;
      }
   }
}
