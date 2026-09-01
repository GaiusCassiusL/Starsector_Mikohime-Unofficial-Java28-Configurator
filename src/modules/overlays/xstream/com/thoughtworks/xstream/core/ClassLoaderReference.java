package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.core.util.CompositeClassLoader;

public final class ClassLoaderReference {
   private transient ClassLoader reference;

   public ClassLoaderReference(ClassLoader reference) {
      this.setReference(reference);
   }

   public ClassLoader getReference() {
      return this.reference;
   }

   public void setReference(ClassLoader reference) {
      this.reference = reference instanceof com.thoughtworks.xstream.core.util.ClassLoaderReference
         ? ((com.thoughtworks.xstream.core.util.ClassLoaderReference)reference).getReference()
         : reference;
   }

   private Object readResolve() {
      this.reference = new CompositeClassLoader();
      return this;
   }
}
