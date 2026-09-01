package com.thoughtworks.xstream.annotations;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.SingleValueConverterWrapper;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class AnnotationReflectionConverter extends ReflectionConverter {
   private final AnnotationProvider annotationProvider;
   private final Map<Class<? extends ConverterMatcher>, Converter> cachedConverters;

   @Deprecated
   public AnnotationReflectionConverter(Mapper mapper, ReflectionProvider reflectionProvider, AnnotationProvider annotationProvider) {
      super(mapper, reflectionProvider);
      this.annotationProvider = annotationProvider;
      this.cachedConverters = new HashMap<>();
   }

   @Override
   protected void marshallField(MarshallingContext context, Object newObj, Field field) {
      XStreamConverter annotation = this.annotationProvider.getAnnotation(field, XStreamConverter.class);
      if (annotation != null) {
         Class<? extends ConverterMatcher> type = annotation.value();
         this.ensureCache(type);
         context.convertAnother(newObj, this.cachedConverters.get(type));
      } else {
         context.convertAnother(newObj);
      }
   }

   private void ensureCache(Class<? extends ConverterMatcher> type) {
      if (!this.cachedConverters.containsKey(type)) {
         this.cachedConverters.put(type, this.newInstance(type));
      }
   }

   @Override
   protected Object unmarshallField(UnmarshallingContext context, Object result, Class type, Field field) {
      XStreamConverter annotation = this.annotationProvider.getAnnotation(field, XStreamConverter.class);
      if (annotation != null) {
         Class<? extends Converter> converterType = annotation.value();
         this.ensureCache(converterType);
         return context.convertAnother(result, type, this.cachedConverters.get(converterType));
      } else {
         return context.convertAnother(result, type);
      }
   }

   private Converter newInstance(Class<? extends ConverterMatcher> type) {
      try {
         Converter converter;
         if (SingleValueConverter.class.isAssignableFrom(type)) {
            SingleValueConverter svc = (SingleValueConverter)type.getConstructor().newInstance();
            converter = new SingleValueConverterWrapper(svc);
         } else {
            converter = (Converter)type.getConstructor().newInstance();
         }

         return converter;
      } catch (InvocationTargetException e) {
         throw new ObjectAccessException("Cannot construct " + type.getName(), e.getCause());
      } catch (InstantiationException e) {
         throw new ObjectAccessException("Cannot construct " + type.getName(), e);
      } catch (IllegalAccessException e) {
         throw new ObjectAccessException("Cannot construct " + type.getName(), e);
      } catch (NoSuchMethodException e) {
         throw new ObjectAccessException("Cannot construct " + type.getName(), e);
      }
   }
}
