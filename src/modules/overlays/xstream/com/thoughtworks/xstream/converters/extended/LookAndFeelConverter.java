package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;
import javax.swing.LookAndFeel;

public class LookAndFeelConverter extends ReflectionConverter {
   public LookAndFeelConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
      super(mapper, reflectionProvider);
   }

   public boolean canConvert(Class type) {
      return type != null && LookAndFeel.class.isAssignableFrom(type) && this.canAccess(type);
   }
}
