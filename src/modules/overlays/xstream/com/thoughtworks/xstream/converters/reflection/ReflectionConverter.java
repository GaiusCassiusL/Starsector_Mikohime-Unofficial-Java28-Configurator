package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.mapper.Mapper;

public class ReflectionConverter extends AbstractReflectionConverter {
   private Class type;

   public ReflectionConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
      super(mapper, reflectionProvider);
   }

   public ReflectionConverter(Mapper mapper, ReflectionProvider reflectionProvider, Class type) {
      this(mapper, reflectionProvider);
      this.type = type;
   }

   public boolean canConvert(Class type) {
      return (this.type != null && this.type == type || this.type == null && type != null) && this.canAccess(type);
   }
}
