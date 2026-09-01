package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.Fields;
import com.thoughtworks.xstream.core.util.PresortedSet;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class TreeSetConverter extends CollectionConverter {
   private transient TreeMapConverter treeMapConverter;

   public TreeSetConverter(Mapper mapper) {
      super(mapper, TreeSet.class);
      this.readResolve();
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      SortedSet sortedSet = (SortedSet)source;
      this.treeMapConverter.marshalComparator(sortedSet.comparator(), writer, context);
      super.marshal(source, writer, context);
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      TreeSet result = null;
      Comparator unmarshalledComparator = this.treeMapConverter.unmarshalComparator(reader, context, null);
      boolean inFirstElement = unmarshalledComparator instanceof Mapper.Null;
      Comparator comparator = inFirstElement ? null : unmarshalledComparator;
      TreeMap treeMap;
      if (TreeSetConverter.Reflections.sortedMapField != null) {
         TreeSet possibleResult = comparator == null ? new TreeSet() : new TreeSet(comparator);
         Object backingMap = null;

         try {
            backingMap = TreeSetConverter.Reflections.sortedMapField.get(possibleResult);
         } catch (IllegalAccessException e) {
            throw new ObjectAccessException("Cannot get backing map of TreeSet", e);
         }

         if (backingMap instanceof TreeMap) {
            treeMap = (TreeMap)backingMap;
            result = possibleResult;
         } else {
            treeMap = null;
         }
      } else {
         treeMap = null;
      }

      if (treeMap == null) {
         PresortedSet set = new PresortedSet(comparator);
         result = comparator == null ? new TreeSet() : new TreeSet(comparator);
         if (inFirstElement) {
            this.addCurrentElementToCollection(reader, context, result, set);
            reader.moveUp();
         }

         this.populateCollection(reader, context, result, set);
         if (set.size() > 0) {
            result.addAll(set);
         }
      } else {
         this.treeMapConverter.populateTreeMap(reader, context, treeMap, unmarshalledComparator);
      }

      return result;
   }

   private Object readResolve() {
      this.treeMapConverter = new TreeSetConverter$1(this, this.mapper());
      return this;
   }

   private static class Reflections {
      private static final Field sortedMapField = JVM.hasOptimizedTreeSetAddAll() ? Fields.locate(TreeSet.class, SortedMap.class, false) : null;
      private static final Object constantValue;

      static {
         Object value = null;
         if (sortedMapField != null) {
            TreeSet set = new TreeSet();
            set.add("1");
            set.add("2");
            Map backingMap = null;

            try {
               backingMap = (Map)sortedMapField.get(set);
            } catch (IllegalAccessException var5) {
            }

            if (backingMap != null) {
               Object[] values = backingMap.values().toArray();
               if (values[0] == values[1]) {
                  value = values[0];
               }
            }
         } else {
            Field valueField = Fields.locate(TreeSet.class, Object.class, true);
            if (valueField != null) {
               try {
                  value = valueField.get(null);
               } catch (IllegalAccessException var4) {
               }
            }
         }

         constantValue = value;
      }
   }
}
