package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.SecurityUtils;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Vector;

public class CollectionConverter extends AbstractCollectionConverter {
   private final Class type;

   public CollectionConverter(Mapper mapper) {
      this(mapper, null);
   }

   public CollectionConverter(Mapper mapper, Class type) {
      super(mapper);
      this.type = type;
      if (type != null && !Collection.class.isAssignableFrom(type)) {
         throw new IllegalArgumentException(type + " not of type " + Collection.class);
      }
   }

   public boolean canConvert(Class type) {
      return this.type != null
         ? type.equals(this.type)
         : type.equals(ArrayList.class)
            || type.equals(HashSet.class)
            || type.equals(LinkedList.class)
            || type.equals(Vector.class)
            || type.equals(LinkedHashSet.class);
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      for (Object item : (Collection)source) {
         this.writeCompleteItem(item, context, writer);
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Collection collection = (Collection)this.createCollection(context.getRequiredType());
      this.populateCollection(reader, context, collection);
      return collection;
   }

   protected void populateCollection(HierarchicalStreamReader reader, UnmarshallingContext context, Collection collection) {
      this.populateCollection(reader, context, collection, collection);
   }

   protected void populateCollection(HierarchicalStreamReader reader, UnmarshallingContext context, Collection collection, Collection target) {
      while (reader.hasMoreChildren()) {
         reader.moveDown();
         this.addCurrentElementToCollection(reader, context, collection, target);
         reader.moveUp();
      }
   }

   protected void addCurrentElementToCollection(HierarchicalStreamReader reader, UnmarshallingContext context, Collection collection, Collection target) {
      Object item = this.readItem(reader, context, collection);
      long now = System.currentTimeMillis();
      target.add(item);
      SecurityUtils.checkForCollectionDoSAttack(context, now);
   }

   protected Object createCollection(Class type) {
      return super.createCollection(this.type != null ? this.type : type);
   }
}
