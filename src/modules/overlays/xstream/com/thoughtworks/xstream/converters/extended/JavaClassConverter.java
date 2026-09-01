package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.DefaultMapper;
import com.thoughtworks.xstream.mapper.Mapper;

public class JavaClassConverter extends AbstractSingleValueConverter {
   private Mapper mapper;

   public JavaClassConverter(ClassLoaderReference classLoaderReference) {
      this(new DefaultMapper(classLoaderReference));
   }

   /** @deprecated */
   public JavaClassConverter(ClassLoader classLoader) {
      this(new ClassLoaderReference(classLoader));
   }

   protected JavaClassConverter(Mapper mapper) {
      this.mapper = mapper;
   }

   public boolean canConvert(Class clazz) {
      return Class.class.equals(clazz);
   }

   public String toString(Object obj) {
      return this.mapper.serializedClass((Class)obj);
   }

   public Object fromString(String str) {
      try {
         return this.mapper.realClass(str);
      } catch (CannotResolveClassException e) {
         throw new ConversionException("Cannot load java class " + str, e.getCause());
      }
   }
}
