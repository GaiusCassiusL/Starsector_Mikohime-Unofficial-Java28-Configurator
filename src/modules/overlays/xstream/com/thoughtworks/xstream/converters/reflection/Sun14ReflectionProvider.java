package com.thoughtworks.xstream.converters.reflection;

/** @deprecated */
public class Sun14ReflectionProvider extends SunUnsafeReflectionProvider {
   /** @deprecated */
   public Sun14ReflectionProvider() {
   }

   /** @deprecated */
   public Sun14ReflectionProvider(FieldDictionary dic) {
      super(dic);
   }

   private Object readResolve() {
      this.init();
      return this;
   }
}
