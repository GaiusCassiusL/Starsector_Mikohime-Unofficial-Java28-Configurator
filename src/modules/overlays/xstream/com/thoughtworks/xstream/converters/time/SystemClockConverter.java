package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.time.Clock;
import java.time.ZoneId;

public class SystemClockConverter implements Converter {
   private final Mapper mapper;
   private final Class<?> type;

   public SystemClockConverter(Mapper mapper) {
      this.mapper = mapper;
      this.type = Clock.systemUTC().getClass();
   }

   @Override
   public boolean canConvert(Class type) {
      return type == this.type;
   }

   @Override
   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Clock clock = (Clock)source;
      ExtendedHierarchicalStreamWriterHelper.startNode(writer, this.mapper.serializedMember(Clock.class, "zone"), null);
      context.convertAnother(clock.getZone());
      writer.endNode();
   }

   @Override
   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      reader.moveDown();
      ZoneId zone = (ZoneId)context.convertAnother(null, ZoneId.class);
      reader.moveUp();
      return Clock.system(zone);
   }
}
