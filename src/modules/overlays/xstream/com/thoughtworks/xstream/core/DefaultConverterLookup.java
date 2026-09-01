package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.core.util.Cloneables;
import com.thoughtworks.xstream.core.util.PrioritizedList;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DefaultConverterLookup implements ConverterLookup, ConverterRegistry, Caching {
   private final PrioritizedList converters = new PrioritizedList();
   private transient Map typeToConverterMap;
   private Map serializationMap = null;

   public DefaultConverterLookup() {
      this(new HashMap());
   }

   public DefaultConverterLookup(Map map) {
      this.typeToConverterMap = map;
      this.typeToConverterMap.clear();
   }

   /** @deprecated */
   public DefaultConverterLookup(Mapper mapper) {
      this();
   }

   public Converter lookupConverterForType(Class type) {
      Converter cachedConverter = type != null ? (Converter)this.typeToConverterMap.get(type.getName()) : null;
      if (cachedConverter != null) {
         return cachedConverter;
      }

      Map errors = new LinkedHashMap();

      for (Converter converter : this.converters) {
         try {
            if (converter.canConvert(type)) {
               if (type != null) {
                  this.typeToConverterMap.put(type.getName(), converter);
               }

               return converter;
            }
         } catch (RuntimeException e) {
            errors.put(converter.getClass().getName(), e.getMessage());
         } catch (LinkageError e) {
            errors.put(converter.getClass().getName(), e.getMessage());
         }
      }

      ConversionException exception = new ConversionException(errors.isEmpty() ? "No converter specified" : "No converter available");
      exception.add("type", type != null ? type.getName() : "null");

      for (Entry entry : errors.entrySet()) {
         exception.add("converter", entry.getKey().toString());
         exception.add("message", entry.getValue().toString());
      }

      throw exception;
   }

   public void registerConverter(Converter converter, int priority) {
      this.typeToConverterMap.clear();
      this.converters.add(converter, priority);
   }

   public void flushCache() {
      this.typeToConverterMap.clear();

      for (Converter converter : this.converters) {
         if (converter instanceof Caching) {
            ((Caching)converter).flushCache();
         }
      }
   }

   private Object writeReplace() {
      this.serializationMap = (Map)Cloneables.cloneIfPossible(this.typeToConverterMap);
      this.serializationMap.clear();
      return this;
   }

   private Object readResolve() {
      this.typeToConverterMap = this.serializationMap == null ? new HashMap() : this.serializationMap;
      this.serializationMap = null;
      return this;
   }
}
