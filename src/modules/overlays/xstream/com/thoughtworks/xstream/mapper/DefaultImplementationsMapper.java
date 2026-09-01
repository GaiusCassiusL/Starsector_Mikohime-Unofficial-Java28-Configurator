package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.InitializationException;
import java.util.HashMap;
import java.util.Map;

public class DefaultImplementationsMapper extends MapperWrapper {
   private final Map typeToImpl = new HashMap();
   private transient Map implToType = new HashMap();

   public DefaultImplementationsMapper(Mapper wrapped) {
      super(wrapped);
      this.addDefaults();
   }

   protected void addDefaults() {
      this.addDefaultImplementation(null, Mapper.Null.class);
      this.addDefaultImplementation(Boolean.class, boolean.class);
      this.addDefaultImplementation(Character.class, char.class);
      this.addDefaultImplementation(Integer.class, int.class);
      this.addDefaultImplementation(Float.class, float.class);
      this.addDefaultImplementation(Double.class, double.class);
      this.addDefaultImplementation(Short.class, short.class);
      this.addDefaultImplementation(Byte.class, byte.class);
      this.addDefaultImplementation(Long.class, long.class);
   }

   public void addDefaultImplementation(Class defaultImplementation, Class ofType) {
      if (defaultImplementation != null && defaultImplementation.isInterface()) {
         throw new InitializationException("Default implementation is not a concrete class: " + defaultImplementation.getName());
      }

      this.typeToImpl.put(ofType, defaultImplementation);
      this.implToType.put(defaultImplementation, ofType);
   }

   public String serializedClass(Class type) {
      Class baseType = (Class)this.implToType.get(type);
      return baseType == null ? super.serializedClass(type) : super.serializedClass(baseType);
   }

   public Class defaultImplementationOf(Class type) {
      return this.typeToImpl.containsKey(type) ? (Class)this.typeToImpl.get(type) : super.defaultImplementationOf(type);
   }

   private Object readResolve() {
      this.implToType = new HashMap();

      for (Object type : this.typeToImpl.keySet()) {
         this.implToType.put(this.typeToImpl.get(type), type);
      }

      return this;
   }
}
