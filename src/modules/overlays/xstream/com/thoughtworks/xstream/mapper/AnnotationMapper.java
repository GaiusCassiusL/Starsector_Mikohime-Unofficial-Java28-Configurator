package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.InitializationException;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAliasType;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamConverters;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamImplicitCollection;
import com.thoughtworks.xstream.annotations.XStreamInclude;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.SingleValueConverterWrapper;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.DependencyInjectionFactory;
import com.thoughtworks.xstream.core.util.TypedNull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnnotationMapper extends MapperWrapper implements AnnotationConfiguration {
   private boolean locked;
   private transient Object[] arguments;
   private final ConverterRegistry converterRegistry;
   private transient ClassAliasingMapper classAliasingMapper;
   private transient DefaultImplementationsMapper defaultImplementationsMapper;
   private transient ImplicitCollectionMapper implicitCollectionMapper;
   private transient FieldAliasingMapper fieldAliasingMapper;
   private transient ElementIgnoringMapper elementIgnoringMapper;
   private transient AttributeMapper attributeMapper;
   private transient LocalConversionMapper localConversionMapper;
   private final Map<Class<?>, Map<List<Object>, Converter>> converterCache = new HashMap<>();
   private final Set<Class<?>> annotatedTypes = Collections.synchronizedSet(new HashSet<>());

   public AnnotationMapper(
      Mapper wrapped,
      ConverterRegistry converterRegistry,
      ConverterLookup converterLookup,
      ClassLoaderReference classLoaderReference,
      ReflectionProvider reflectionProvider
   ) {
      super(wrapped);
      this.converterRegistry = converterRegistry;
      this.annotatedTypes.add(Object.class);
      this.setupMappers();
      this.locked = true;
      ClassLoader classLoader = classLoaderReference.getReference();
      this.arguments = new Object[]{
         this, classLoaderReference, reflectionProvider, converterLookup, new JVM(), classLoader != null ? classLoader : new TypedNull(ClassLoader.class)
      };
   }

   /** @deprecated */
   public AnnotationMapper(
      Mapper wrapped,
      ConverterRegistry converterRegistry,
      ConverterLookup converterLookup,
      ClassLoader classLoader,
      ReflectionProvider reflectionProvider,
      JVM jvm
   ) {
      this(wrapped, converterRegistry, converterLookup, new ClassLoaderReference(classLoader), reflectionProvider);
   }

   @Override
   public String realMember(Class type, String serialized) {
      if (!this.locked) {
         this.processAnnotations(type);
      }

      return super.realMember(type, serialized);
   }

   @Override
   public String serializedClass(Class type) {
      if (!this.locked) {
         this.processAnnotations(type);
      }

      return super.serializedClass(type);
   }

   @Override
   public Class defaultImplementationOf(Class type) {
      if (!this.locked) {
         this.processAnnotations(type);
      }

      Class defaultImplementation = super.defaultImplementationOf(type);
      if (!this.locked) {
         this.processAnnotations(defaultImplementation);
      }

      return defaultImplementation;
   }

   @Override
   public Converter getLocalConverter(Class definedIn, String fieldName) {
      if (!this.locked) {
         this.processAnnotations(definedIn);
      }

      return super.getLocalConverter(definedIn, fieldName);
   }

   @Override
   public void autodetectAnnotations(boolean mode) {
      this.locked = !mode;
   }

   @Override
   public void processAnnotations(Class[] initialTypes) {
      if (initialTypes != null && initialTypes.length != 0) {
         this.locked = true;
         Set<Class<?>> types = new AnnotationMapper.UnprocessedTypesSet();

         for (Class initialType : initialTypes) {
            types.add(initialType);
         }

         this.processTypes(types);
      }
   }

   private void processAnnotations(Class initialType) {
      if (initialType != null) {
         Set<Class<?>> types = new AnnotationMapper.UnprocessedTypesSet();
         types.add(initialType);
         this.processTypes(types);
      }
   }

   private void processTypes(Set<Class<?>> types) {
      while (!types.isEmpty()) {
         Iterator<Class<?>> iter = types.iterator();
         Class<?> type = iter.next();
         iter.remove();
         synchronized (type) {
            if (!this.annotatedTypes.contains(type)) {
               try {
                  if (!type.isPrimitive()) {
                     this.addParametrizedTypes(type, types);
                     this.processConverterAnnotations(type);
                     this.processAliasAnnotation(type, types);
                     this.processAliasTypeAnnotation(type);
                     if (!type.isInterface()) {
                        this.processImplicitCollectionAnnotation(type);
                        Field[] fields = type.getDeclaredFields();

                        for (int i = 0; i < fields.length; i++) {
                           Field field = fields[i];
                           if (!field.isEnumConstant() && (field.getModifiers() & 136) <= 0) {
                              this.addParametrizedTypes(field.getGenericType(), types);
                              if (!field.isSynthetic()) {
                                 this.processFieldAliasAnnotation(field);
                                 this.processAsAttributeAnnotation(field);
                                 this.processImplicitAnnotation(field);
                                 this.processOmitFieldAnnotation(field);
                                 this.processLocalConverterAnnotation(field);
                              }
                           }
                        }
                     }
                  }
               } finally {
                  this.annotatedTypes.add(type);
               }
            }
         }
      }
   }

   private void addParametrizedTypes(Type type, final Set<Class<?>> types) {
      final Set<Type> processedTypes = new HashSet<>();
      Set<Type> localTypes = new LinkedHashSet<Type>() {
         public boolean add(Type o) {
            if (o instanceof Class) {
               return types.add((Class<?>)o);
            } else {
               return o != null && !processedTypes.contains(o) ? super.add(o) : false;
            }
         }
      };

      while (type != null) {
         processedTypes.add(type);
         if (!(type instanceof Class)) {
            if (type instanceof TypeVariable) {
               TypeVariable<?> typeVariable = (TypeVariable<?>)type;
               Type[] bounds = typeVariable.getBounds();

               for (Type bound : bounds) {
                  localTypes.add(bound);
               }
            } else if (type instanceof ParameterizedType) {
               ParameterizedType parametrizedType = (ParameterizedType)type;
               localTypes.add(parametrizedType.getRawType());
               Type[] actualArguments = parametrizedType.getActualTypeArguments();

               for (Type actualArgument : actualArguments) {
                  localTypes.add(actualArgument);
               }
            } else if (type instanceof GenericArrayType) {
               GenericArrayType arrayType = (GenericArrayType)type;
               localTypes.add(arrayType.getGenericComponentType());
            }
         } else {
            Class<?> clazz = (Class<?>)type;
            types.add(clazz);
            if (!clazz.isPrimitive()) {
               TypeVariable<?>[] typeParameters = clazz.getTypeParameters();

               for (TypeVariable<?> typeVariable : typeParameters) {
                  localTypes.add(typeVariable);
               }

               localTypes.add(clazz.getGenericSuperclass());

               for (Type iface : clazz.getGenericInterfaces()) {
                  localTypes.add(iface);
               }
            }
         }

         if (!localTypes.isEmpty()) {
            Iterator<Type> iter = localTypes.iterator();
            type = iter.next();
            iter.remove();
         } else {
            type = null;
         }
      }
   }

   private void processConverterAnnotations(Class<?> type) {
      if (this.converterRegistry != null) {
         XStreamConverters convertersAnnotation = type.getAnnotation(XStreamConverters.class);
         XStreamConverter converterAnnotation = type.getAnnotation(XStreamConverter.class);
         List<XStreamConverter> annotations = convertersAnnotation != null ? new ArrayList<>(Arrays.asList(convertersAnnotation.value())) : new ArrayList<>();
         if (converterAnnotation != null) {
            annotations.add(converterAnnotation);
         }

         for (XStreamConverter annotation : annotations) {
            Converter converter = this.cacheConverter(annotation, converterAnnotation != null ? type : null);
            if (converter != null) {
               if (converterAnnotation == null && !converter.canConvert(type)) {
                  throw new InitializationException("Converter " + annotation.value().getName() + " cannot handle annotated class " + type.getName());
               }

               this.converterRegistry.registerConverter(converter, annotation.priority());
            }
         }
      }
   }

   private void processAliasAnnotation(Class<?> type, Set<Class<?>> types) {
      XStreamAlias aliasAnnotation = type.getAnnotation(XStreamAlias.class);
      if (aliasAnnotation != null) {
         if (this.classAliasingMapper == null) {
            throw new InitializationException("No " + ClassAliasingMapper.class.getName() + " available");
         }

         this.classAliasingMapper.addClassAlias(aliasAnnotation.value(), type);
         if (aliasAnnotation.impl() != Void.class) {
            this.defaultImplementationsMapper.addDefaultImplementation(aliasAnnotation.impl(), type);
            if (type.isInterface()) {
               types.add(aliasAnnotation.impl());
            }
         }
      }
   }

   private void processAliasTypeAnnotation(Class<?> type) {
      XStreamAliasType aliasAnnotation = type.getAnnotation(XStreamAliasType.class);
      if (aliasAnnotation != null) {
         if (this.classAliasingMapper == null) {
            throw new InitializationException("No " + ClassAliasingMapper.class.getName() + " available");
         }

         this.classAliasingMapper.addTypeAlias(aliasAnnotation.value(), type);
      }
   }

   @Deprecated
   private void processImplicitCollectionAnnotation(Class<?> type) {
      XStreamImplicitCollection implicitColAnnotation = type.getAnnotation(XStreamImplicitCollection.class);
      if (implicitColAnnotation != null) {
         if (this.implicitCollectionMapper == null) {
            throw new InitializationException("No " + ImplicitCollectionMapper.class.getName() + " available");
         }

         String fieldName = implicitColAnnotation.value();
         String itemFieldName = implicitColAnnotation.item();

         Field field;
         try {
            field = type.getDeclaredField(fieldName);
         } catch (NoSuchFieldException e) {
            throw new InitializationException(
               type.getName() + " does not have a field named '" + fieldName + "' as required by " + XStreamImplicitCollection.class.getName()
            );
         }

         Class itemType = null;
         Type genericType = field.getGenericType();
         if (genericType instanceof ParameterizedType) {
            Type typeArgument = ((ParameterizedType)genericType).getActualTypeArguments()[0];
            itemType = this.getClass(typeArgument);
         }

         if (itemType == null) {
            this.implicitCollectionMapper.add(type, fieldName, null, Object.class);
         } else if (itemFieldName.equals("")) {
            this.implicitCollectionMapper.add(type, fieldName, null, itemType);
         } else {
            this.implicitCollectionMapper.add(type, fieldName, itemFieldName, itemType);
         }
      }
   }

   private void processFieldAliasAnnotation(Field field) {
      XStreamAlias aliasAnnotation = field.getAnnotation(XStreamAlias.class);
      if (aliasAnnotation != null) {
         if (this.fieldAliasingMapper == null) {
            throw new InitializationException("No " + FieldAliasingMapper.class.getName() + " available");
         }

         this.fieldAliasingMapper.addFieldAlias(aliasAnnotation.value(), field.getDeclaringClass(), field.getName());
      }
   }

   private void processAsAttributeAnnotation(Field field) {
      XStreamAsAttribute asAttributeAnnotation = field.getAnnotation(XStreamAsAttribute.class);
      if (asAttributeAnnotation != null) {
         if (this.attributeMapper == null) {
            throw new InitializationException("No " + AttributeMapper.class.getName() + " available");
         }

         this.attributeMapper.addAttributeFor(field);
      }
   }

   private void processImplicitAnnotation(Field field) {
      XStreamImplicit implicitAnnotation = field.getAnnotation(XStreamImplicit.class);
      if (implicitAnnotation != null) {
         if (this.implicitCollectionMapper == null) {
            throw new InitializationException("No " + ImplicitCollectionMapper.class.getName() + " available");
         }

         String fieldName = field.getName();
         String itemFieldName = implicitAnnotation.itemFieldName();
         String keyFieldName = implicitAnnotation.keyFieldName();
         boolean isMap = Map.class.isAssignableFrom(field.getType());
         Class itemType = null;
         if (!field.getType().isArray()) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
               Type[] actualTypeArguments = ((ParameterizedType)genericType).getActualTypeArguments();
               Type typeArgument = actualTypeArguments[isMap ? 1 : 0];
               itemType = this.getClass(typeArgument);
            }
         }

         if (isMap) {
            this.implicitCollectionMapper
               .add(
                  field.getDeclaringClass(),
                  fieldName,
                  itemFieldName != null && !"".equals(itemFieldName) ? itemFieldName : null,
                  itemType,
                  keyFieldName != null && !"".equals(keyFieldName) ? keyFieldName : null
               );
         } else if (itemFieldName != null && !"".equals(itemFieldName)) {
            this.implicitCollectionMapper.add(field.getDeclaringClass(), fieldName, itemFieldName, itemType);
         } else {
            this.implicitCollectionMapper.add(field.getDeclaringClass(), fieldName, itemType);
         }
      }
   }

   private void processOmitFieldAnnotation(Field field) {
      XStreamOmitField omitFieldAnnotation = field.getAnnotation(XStreamOmitField.class);
      if (omitFieldAnnotation != null) {
         if (this.elementIgnoringMapper == null) {
            throw new InitializationException("No " + ElementIgnoringMapper.class.getName() + " available");
         }

         this.elementIgnoringMapper.omitField(field.getDeclaringClass(), field.getName());
      }
   }

   private void processLocalConverterAnnotation(Field field) {
      XStreamConverter annotation = field.getAnnotation(XStreamConverter.class);
      if (annotation != null) {
         Converter converter = this.cacheConverter(annotation, field.getType());
         if (converter != null) {
            if (this.localConversionMapper == null) {
               throw new InitializationException("No " + LocalConversionMapper.class.getName() + " available");
            }

            this.localConversionMapper.registerLocalConverter(field.getDeclaringClass(), field.getName(), converter);
         }
      }
   }

   private Converter cacheConverter(XStreamConverter annotation, Class targetType) {
      Converter result = null;
      List<Object> parameter = new ArrayList<>();
      if (targetType != null && annotation.useImplicitType()) {
         parameter.add(targetType);
      }

      List<Object> arrays = new ArrayList<>();
      arrays.add(annotation.booleans());
      arrays.add(annotation.bytes());
      arrays.add(annotation.chars());
      arrays.add(annotation.doubles());
      arrays.add(annotation.floats());
      arrays.add(annotation.ints());
      arrays.add(annotation.longs());
      arrays.add(annotation.shorts());
      arrays.add(annotation.strings());
      arrays.add(annotation.types());

      for (Object array : arrays) {
         if (array != null) {
            int length = Array.getLength(array);

            for (int i = 0; i < length; i++) {
               parameter.add(Array.get(array, i));
            }
         }
      }

      for (Class<?> type : annotation.nulls()) {
         TypedNull nullType = new TypedNull(type);
         parameter.add(nullType);
      }

      Class<? extends ConverterMatcher> converterType = annotation.value();
      Map<List<Object>, Converter> converterMapping = this.converterCache.get(converterType);
      if (converterMapping != null) {
         result = converterMapping.get(parameter);
      }

      if (result == null) {
         int size = parameter.size();
         Object[] args;
         if (size > 0) {
            args = new Object[this.arguments.length + size];
            System.arraycopy(this.arguments, 0, args, size, this.arguments.length);
            System.arraycopy(parameter.toArray(new Object[size]), 0, args, 0, size);
         } else {
            args = this.arguments;
         }

         Converter converter;
         try {
            if (SingleValueConverter.class.isAssignableFrom(converterType) && !Converter.class.isAssignableFrom(converterType)) {
               SingleValueConverter svc = (SingleValueConverter)DependencyInjectionFactory.newInstance(converterType, args);
               converter = new SingleValueConverterWrapper(svc);
            } else {
               converter = (Converter)DependencyInjectionFactory.newInstance(converterType, args);
            }
         } catch (Exception e) {
            throw new InitializationException(
               "Cannot instantiate converter " + converterType.getName() + (targetType != null ? " for type " + targetType.getName() : ""), e
            );
         }

         if (converterMapping == null) {
            converterMapping = new HashMap<>();
            this.converterCache.put(converterType, converterMapping);
         }

         converterMapping.put(parameter, converter);
         result = converter;
      }

      return result;
   }

   private Class<?> getClass(Type typeArgument) {
      Class<?> type = null;
      if (typeArgument instanceof ParameterizedType) {
         type = (Class<?>)((ParameterizedType)typeArgument).getRawType();
      } else if (typeArgument instanceof Class) {
         type = (Class<?>)typeArgument;
      }

      return type;
   }

   private void setupMappers() {
      this.classAliasingMapper = (ClassAliasingMapper)this.lookupMapperOfType(ClassAliasingMapper.class);
      this.defaultImplementationsMapper = (DefaultImplementationsMapper)this.lookupMapperOfType(DefaultImplementationsMapper.class);
      this.implicitCollectionMapper = (ImplicitCollectionMapper)this.lookupMapperOfType(ImplicitCollectionMapper.class);
      this.fieldAliasingMapper = (FieldAliasingMapper)this.lookupMapperOfType(FieldAliasingMapper.class);
      this.elementIgnoringMapper = (ElementIgnoringMapper)this.lookupMapperOfType(ElementIgnoringMapper.class);
      this.attributeMapper = (AttributeMapper)this.lookupMapperOfType(AttributeMapper.class);
      this.localConversionMapper = (LocalConversionMapper)this.lookupMapperOfType(LocalConversionMapper.class);
   }

   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      int max = this.arguments.length - 2;
      out.writeInt(max);

      for (int i = 0; i < max; i++) {
         out.writeObject(this.arguments[i]);
      }
   }

   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      this.setupMappers();
      int max = in.readInt();
      this.arguments = new Object[max + 2];

      for (int i = 0; i < max; i++) {
         this.arguments[i] = in.readObject();
         if (this.arguments[i] instanceof ClassLoaderReference) {
            this.arguments[max + 1] = ((ClassLoaderReference)this.arguments[i]).getReference();
         }
      }

      this.arguments[max] = new JVM();
   }

   private final class UnprocessedTypesSet extends LinkedHashSet<Class<?>> {
      private UnprocessedTypesSet() {
      }

      public boolean add(Class<?> type) {
         if (type == null) {
            return false;
         }

         while (type.isArray()) {
            type = type.getComponentType();
         }

         String name = type.getName();
         if (!name.startsWith("java.") && !name.startsWith("javax.")) {
            boolean ret = AnnotationMapper.this.annotatedTypes.contains(type) ? false : super.add(type);
            if (ret) {
               XStreamInclude inc = type.getAnnotation(XStreamInclude.class);
               if (inc != null) {
                  Class<?>[] incTypes = inc.value();
                  if (incTypes != null) {
                     for (Class<?> incType : incTypes) {
                        this.add(incType);
                     }
                  }
               }
            }

            return ret;
         } else {
            return false;
         }
      }
   }
}
