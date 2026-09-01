package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.Types;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.Serializable;

public class LambdaConverter extends SerializableConverter {
   public LambdaConverter(Mapper mapper, ReflectionProvider reflectionProvider, ClassLoaderReference classLoaderReference) {
      super(mapper, reflectionProvider, classLoaderReference);
   }

   @Override
   public boolean canConvert(Class type) {
      return Types.isLambdaType(type) && (JVM.canCreateDerivedObjectOutputStream() || !Serializable.class.isAssignableFrom(type));
   }

   @Override
   public void marshal(Object original, HierarchicalStreamWriter writer, MarshallingContext context) {
      if (original instanceof Serializable) {
         super.marshal(original, writer, context);
      }
   }
}
