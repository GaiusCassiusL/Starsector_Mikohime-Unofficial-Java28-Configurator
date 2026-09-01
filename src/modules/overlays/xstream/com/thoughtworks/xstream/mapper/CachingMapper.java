package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.core.Caching;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CachingMapper extends MapperWrapper implements Caching {
   private transient Map realClassCache;

   public CachingMapper(Mapper wrapped) {
      super(wrapped);
      this.readResolve();
   }

   public Class realClass(String elementName) {
      Object cached = this.realClassCache.get(elementName);
      if (cached != null) {
         if (cached instanceof Class) {
            return (Class)cached;
         } else {
            throw (XStreamException)cached;
         }
      } else {
         try {
            Class result = super.realClass(elementName);
            this.realClassCache.put(elementName, result);
            return result;
         } catch (ForbiddenClassException e) {
            this.realClassCache.put(elementName, e);
            throw e;
         } catch (CannotResolveClassException e) {
            this.realClassCache.put(elementName, e);
            throw e;
         }
      }
   }

   public void flushCache() {
      this.realClassCache.clear();
   }

   private Object readResolve() {
      this.realClassCache = Collections.synchronizedMap(new HashMap(128));
      return this;
   }
}
