package com.thoughtworks.xstream.mapper;

import java.util.HashSet;
import java.util.Set;

public class ImmutableTypesMapper extends MapperWrapper {
   private final Set unreferenceableTypes = new HashSet();
   private final Set immutableTypes = new HashSet();

   public ImmutableTypesMapper(Mapper wrapped) {
      super(wrapped);
   }

   /** @deprecated */
   public void addImmutableType(Class type) {
      this.addImmutableType(type, true);
   }

   public void addImmutableType(Class type, boolean isReferenceable) {
      this.immutableTypes.add(type);
      if (!isReferenceable) {
         this.unreferenceableTypes.add(type);
      } else {
         this.unreferenceableTypes.remove(type);
      }
   }

   public boolean isImmutableValueType(Class type) {
      return this.immutableTypes.contains(type) ? true : super.isImmutableValueType(type);
   }

   public boolean isReferenceable(Class type) {
      return this.immutableTypes.contains(type) ? !this.unreferenceableTypes.contains(type) : super.isReferenceable(type);
   }
}
