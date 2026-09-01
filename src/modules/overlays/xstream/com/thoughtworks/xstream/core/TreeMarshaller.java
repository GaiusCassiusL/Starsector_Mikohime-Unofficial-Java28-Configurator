package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.core.util.ObjectIdDictionary;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Collections;
import java.util.Iterator;

public class TreeMarshaller implements MarshallingContext {
   protected HierarchicalStreamWriter writer;
   protected ConverterLookup converterLookup;
   private Mapper mapper;
   private ObjectIdDictionary parentObjects = new ObjectIdDictionary();
   private DataHolder dataHolder;

   public TreeMarshaller(HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper mapper) {
      this.writer = writer;
      this.converterLookup = converterLookup;
      this.mapper = mapper;
   }

   public void convertAnother(Object item) {
      this.convertAnother(item, null);
   }

   public void convertAnother(Object item, Converter converter) {
      if (converter == null) {
         converter = this.converterLookup.lookupConverterForType(item.getClass());
      } else if (!converter.canConvert(item.getClass())) {
         ConversionException e = new ConversionException("Explicit selected converter cannot handle item");
         e.add("item-type", item.getClass().getName());
         e.add("converter-type", converter.getClass().getName());
         throw e;
      }

      this.convert(item, converter);
   }

   protected void convert(Object item, Converter converter) {
      if (this.parentObjects.containsId(item)) {
         ConversionException e = new TreeMarshaller.CircularReferenceException("Recursive reference to parent object");
         e.add("item-type", item.getClass().getName());
         e.add("converter-type", converter.getClass().getName());
         throw e;
      }

      this.parentObjects.associateId(item, "");
      converter.marshal(item, this.writer, this);
      this.parentObjects.removeId(item);
   }

   public void start(Object item, DataHolder dataHolder) {
      this.dataHolder = dataHolder;
      if (item == null) {
         this.writer.startNode(this.mapper.serializedClass(null));
         this.writer.endNode();
      } else {
         ExtendedHierarchicalStreamWriterHelper.startNode(this.writer, this.mapper.serializedClass(item.getClass()), item.getClass());
         this.convertAnother(item);
         this.writer.endNode();
      }
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

   protected Mapper getMapper() {
      return this.mapper;
   }

   public static class CircularReferenceException extends ConversionException {
      public CircularReferenceException(String msg) {
         super(msg);
      }
   }
}
