package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.core.Caching;
import com.thoughtworks.xstream.core.util.SerializationMembers;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** @deprecated */
public class SerializationMethodInvoker implements Caching {
   SerializationMembers serializationMembers = new SerializationMembers();

   /** @deprecated */
   public Object callReadResolve(Object result) {
      return this.serializationMembers.callReadResolve(result);
   }

   /** @deprecated */
   public Object callWriteReplace(Object object) {
      return this.serializationMembers.callWriteReplace(object);
   }

   /** @deprecated */
   public boolean supportsReadObject(Class type, boolean includeBaseClasses) {
      return this.serializationMembers.supportsReadObject(type, includeBaseClasses);
   }

   /** @deprecated */
   public void callReadObject(Class type, Object object, ObjectInputStream stream) {
      this.serializationMembers.callReadObject(type, object, stream);
   }

   /** @deprecated */
   public boolean supportsWriteObject(Class type, boolean includeBaseClasses) {
      return this.serializationMembers.supportsWriteObject(type, includeBaseClasses);
   }

   /** @deprecated */
   public void callWriteObject(Class type, Object instance, ObjectOutputStream stream) {
      this.serializationMembers.callWriteObject(type, instance, stream);
   }

   /** @deprecated */
   public void flushCache() {
      this.serializationMembers.flushCache();
   }
}
