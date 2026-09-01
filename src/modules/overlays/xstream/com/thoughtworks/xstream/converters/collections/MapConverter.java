package com.thoughtworks.xstream.converters.collections;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.SecurityUtils;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

public class MapConverter extends AbstractCollectionConverter {
   private final Class type;

   public MapConverter(Mapper mapper) {
      this(mapper, null);
   }

   public MapConverter(Mapper mapper, Class type) {
      super(mapper);
      this.type = type;
      if (type != null && !Map.class.isAssignableFrom(type)) {
         throw new IllegalArgumentException(type + " not of type " + Map.class);
      }
   }

   public boolean canConvert(Class type) {
      return this.type != null
         ? type.equals(this.type)
         : type.equals(HashMap.class)
            || type.equals(Hashtable.class)
            || type.getName().equals("java.util.LinkedHashMap")
            || type.getName().equals("java.util.concurrent.ConcurrentHashMap")
            || type.getName().equals("sun.font.AttributeMap");
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Map map = (Map)source;
      String entryName = this.mapper().serializedClass(Entry.class);

      for (Entry entry : map.entrySet()) {
         ExtendedHierarchicalStreamWriterHelper.startNode(writer, entryName, entry.getClass());
         this.writeCompleteItem(entry.getKey(), context, writer);
         this.writeCompleteItem(entry.getValue(), context, writer);
         writer.endNode();
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Map map = (Map)this.createCollection(context.getRequiredType());
      this.populateMap(reader, context, map);
      return map;
   }

   protected void populateMap(HierarchicalStreamReader reader, UnmarshallingContext context, Map map) {
      this.populateMap(reader, context, map, map);
   }

   protected void populateMap(HierarchicalStreamReader reader, UnmarshallingContext context, Map map, Map target) {
      while (reader.hasMoreChildren()) {
         reader.moveDown();
         this.putCurrentEntryIntoMap(reader, context, map, target);
         reader.moveUp();
      }
   }

   protected void putCurrentEntryIntoMap(HierarchicalStreamReader reader, UnmarshallingContext context, Map map, Map target) {
      Object key = this.readCompleteItem(reader, context, map);
      Object value = this.readCompleteItem(reader, context, map);
      long now = System.currentTimeMillis();
      target.put(key, value);
      SecurityUtils.checkForCollectionDoSAttack(context, now);
   }

   protected Object createCollection(Class type) {
      return super.createCollection(this.type != null ? this.type : type);
   }
}
