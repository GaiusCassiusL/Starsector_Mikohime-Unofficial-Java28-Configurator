package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractReferenceUnmarshaller extends TreeUnmarshaller {
   private static final Object NULL = new Object();
   private Map values = new HashMap();
   private FastStack parentStack = new FastStack(16);

   public AbstractReferenceUnmarshaller(Object root, HierarchicalStreamReader reader, ConverterLookup converterLookup, Mapper mapper) {
      super(root, reader, converterLookup, mapper);
   }

   protected Object convert(Object parent, Class type, Converter converter) {
      Object parentReferenceKey;
      if (this.parentStack.size() > 0) {
         parentReferenceKey = this.parentStack.peek();
         if (parentReferenceKey != null && !this.values.containsKey(parentReferenceKey)) {
            this.values.put(parentReferenceKey, parent);
         }
      }

      String attributeName = this.getMapper().aliasForSystemAttribute("reference");
      String reference = attributeName == null ? null : this.reader.getAttribute(attributeName);
      boolean isReferenceable = this.getMapper().isReferenceable(type);
      if (reference != null) {
         Object cache = isReferenceable ? this.values.get(this.getReferenceKey(reference)) : null;
         if (cache == null) {
            ConversionException ex = new ConversionException("Invalid reference");
            ex.add("reference", reference);
            ex.add("referenced-type", type.getName());
            ex.add("referenceable", Boolean.toString(isReferenceable));
            throw ex;
         }

         parentReferenceKey = cache == NULL ? null : cache;
      } else if (!isReferenceable) {
         parentReferenceKey = super.convert(parent, type, converter);
      } else {
         Object currentReferenceKey = this.getCurrentReferenceKey();
         this.parentStack.push(currentReferenceKey);
         Object localResult = null;

         try {
            localResult = super.convert(parent, type, converter);
         } finally {
            parentReferenceKey = localResult;
            if (currentReferenceKey != null) {
               this.values.put(currentReferenceKey, parentReferenceKey == null ? NULL : parentReferenceKey);
            }

            this.parentStack.popSilently();
         }
      }

      return parentReferenceKey;
   }

   protected abstract Object getReferenceKey(String var1);

   protected abstract Object getCurrentReferenceKey();
}
