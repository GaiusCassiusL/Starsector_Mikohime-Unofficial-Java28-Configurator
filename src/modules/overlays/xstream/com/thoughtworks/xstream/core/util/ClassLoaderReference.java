package com.thoughtworks.xstream.core.util;

/** @deprecated */
public class ClassLoaderReference extends ClassLoader {
   private transient ClassLoader reference;

   /** @deprecated */
   public ClassLoaderReference(ClassLoader reference) {
      this.reference = reference;
   }

   /** @deprecated */
   public Class loadClass(String name) throws ClassNotFoundException {
      return this.reference.loadClass(name);
   }

   /** @deprecated */
   public ClassLoader getReference() {
      return this.reference;
   }

   /** @deprecated */
   public void setReference(ClassLoader reference) {
      this.reference = reference;
   }

   private Object writeReplace() {
      return new ClassLoaderReference.Replacement();
   }

   static class Replacement {
      private Object readResolve() {
         return new ClassLoaderReference(new CompositeClassLoader());
      }
   }
}
