package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DurationConverter extends AbstractSingleValueConverter {
   private final DatatypeFactory factory;

   public DurationConverter() {
      this(new DurationConverter$1().getFactory());
   }

   public DurationConverter(DatatypeFactory factory) {
      this.factory = factory;
   }

   public boolean canConvert(Class type) {
      return this.factory != null && type != null && Duration.class.isAssignableFrom(type);
   }

   public Object fromString(String s) {
      return this.factory.newDuration(s);
   }
}
