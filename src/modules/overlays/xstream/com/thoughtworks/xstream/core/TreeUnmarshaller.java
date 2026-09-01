package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.ErrorReporter;
import com.thoughtworks.xstream.converters.ErrorWriter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.core.util.PrioritizedList;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.security.AbstractSecurityException;
import java.util.Collections;
import java.util.Iterator;

public class TreeUnmarshaller implements UnmarshallingContext {
   private Object root;
   protected HierarchicalStreamReader reader;
   private ConverterLookup converterLookup;
   private Mapper mapper;
   private FastStack types = new FastStack(16);
   private DataHolder dataHolder;
   private final PrioritizedList validationList = new PrioritizedList();

   public TreeUnmarshaller(Object root, HierarchicalStreamReader reader, ConverterLookup converterLookup, Mapper mapper) {
      this.root = root;
      this.reader = reader;
      this.converterLookup = converterLookup;
      this.mapper = mapper;
   }

   public Object convertAnother(Object parent, Class type) {
      return this.convertAnother(parent, type, null);
   }

   public Object convertAnother(Object parent, Class type, Converter converter) {
      type = this.mapper.defaultImplementationOf(type);
      if (converter == null) {
         converter = this.converterLookup.lookupConverterForType(type);
      } else if (!converter.canConvert(type)) {
         ConversionException e = new ConversionException("Explicit selected converter cannot handle type");
         e.add("item-type", type.getName());
         e.add("converter-type", converter.getClass().getName());
         throw e;
      }

      return this.convert(parent, type, converter);
   }

   protected Object convert(Object parent, Class type, Converter converter) {
      this.types.push(type);

      try {
         return converter.unmarshal(this.reader, this);
      } catch (ConversionException conversionException) {
         this.addInformationTo(conversionException, type, converter, parent);
         throw conversionException;
      } catch (AbstractSecurityException e) {
         throw e;
      } catch (RuntimeException e) {
         ConversionException conversionException = new ConversionException(e);
         this.addInformationTo(conversionException, type, converter, parent);
         throw conversionException;
      } finally {
         this.types.popSilently();
      }
   }

   private void addInformationTo(ErrorWriter errorWriter, Class type, Converter converter, Object parent) {
      errorWriter.add("class", type.getName());
      errorWriter.add("required-type", this.getRequiredType().getName());
      errorWriter.add("converter-type", converter.getClass().getName());
      if (converter instanceof ErrorReporter) {
         ((ErrorReporter)converter).appendErrors(errorWriter);
      }

      if (parent instanceof ErrorReporter) {
         ((ErrorReporter)parent).appendErrors(errorWriter);
      }

      this.reader.appendErrors(errorWriter);
   }

   public void addCompletionCallback(Runnable work, int priority) {
      this.validationList.add(work, priority);
   }

   public Object currentObject() {
      return this.types.size() == 1 ? this.root : null;
   }

   public Class getRequiredType() {
      return (Class)this.types.peek();
   }

   public Object get(Object key) {
      return this.dataHolder != null ? this.dataHolder.get(key) : null;
   }

   public void put(Object key, Object value) {
      this.lazilyCreateDataHolder();
      this.dataHolder.put(key, value);
   }

   public Iterator keys() {
      return this.dataHolder != null ? this.dataHolder.keys() : Collections.EMPTY_MAP.keySet().iterator();
   }

   private void lazilyCreateDataHolder() {
      if (this.dataHolder == null) {
         this.dataHolder = new MapBackedDataHolder();
      }
   }

   public Object start(DataHolder dataHolder) {
      this.dataHolder = dataHolder;
      Class type = HierarchicalStreams.readClassType(this.reader, this.mapper);
      Object result = this.convertAnother(null, type);

      for (Runnable runnable : this.validationList) {
         runnable.run();
      }

      return result;
   }

   protected Mapper getMapper() {
      return this.mapper;
   }
}
