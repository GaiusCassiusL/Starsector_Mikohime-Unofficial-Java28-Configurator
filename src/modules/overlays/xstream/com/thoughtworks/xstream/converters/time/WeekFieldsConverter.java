package com.thoughtworks.xstream.converters.time;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.time.DayOfWeek;
import java.time.temporal.WeekFields;

public class WeekFieldsConverter implements Converter {
   private final Mapper mapper;

   public WeekFieldsConverter(Mapper mapper) {
      this.mapper = mapper;
   }

   @Override
   public boolean canConvert(Class type) {
      return type == WeekFields.class;
   }

   @Override
   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      WeekFields weekFields = (WeekFields)source;
      ExtendedHierarchicalStreamWriterHelper.startNode(writer, this.mapper.serializedMember(WeekFields.class, "minimalDays"), int.class);
      writer.setValue(String.valueOf(weekFields.getMinimalDaysInFirstWeek()));
      writer.endNode();
      ExtendedHierarchicalStreamWriterHelper.startNode(writer, this.mapper.serializedMember(WeekFields.class, "firstDayOfWeek"), DayOfWeek.class);
      context.convertAnother(weekFields.getFirstDayOfWeek());
      writer.endNode();
   }

   @Override
   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      boolean oldFormat = "custom".equals(reader.getAttribute(this.mapper.aliasForSystemAttribute("serialization")));
      if (oldFormat) {
         reader.moveDown();
         reader.moveDown();
      }

      int minimalDays = 0;
      DayOfWeek firstDayOfWeek = null;

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         String name = oldFormat ? reader.getNodeName() : this.mapper.realMember(WeekFields.class, reader.getNodeName());
         if ("minimalDays".equals(name)) {
            minimalDays = Integer.parseInt(reader.getValue());
         } else {
            if (!"firstDayOfWeek".equals(name)) {
               throw new AbstractReflectionConverter.UnknownFieldException(WeekFields.class.getName(), name);
            }

            firstDayOfWeek = (DayOfWeek)context.convertAnother(null, DayOfWeek.class);
         }

         reader.moveUp();
      }

      if (oldFormat) {
         reader.moveUp();
         reader.moveUp();
      }

      return WeekFields.of(firstDayOfWeek, minimalDays);
   }
}
