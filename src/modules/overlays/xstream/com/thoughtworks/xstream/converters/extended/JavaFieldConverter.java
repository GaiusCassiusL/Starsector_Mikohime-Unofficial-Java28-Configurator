package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.DefaultMapper;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;

public class JavaFieldConverter implements Converter {
   private final SingleValueConverter javaClassConverter;
   private final Mapper mapper;

   public JavaFieldConverter(ClassLoaderReference classLoaderReference) {
      this(new JavaClassConverter(classLoaderReference), new DefaultMapper(classLoaderReference));
   }

   /** @deprecated */
   public JavaFieldConverter(ClassLoader classLoader) {
      this(new ClassLoaderReference(classLoader));
   }

   protected JavaFieldConverter(SingleValueConverter javaClassConverter, Mapper mapper) {
      this.javaClassConverter = javaClassConverter;
      this.mapper = mapper;
   }

   public boolean canConvert(Class type) {
      return type == Field.class;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Field field = (Field)source;
      Class type = field.getDeclaringClass();
      writer.startNode("name");
      writer.setValue(this.mapper.serializedMember(type, field.getName()));
      writer.endNode();
      writer.startNode("clazz");
      writer.setValue(this.javaClassConverter.toString(type));
      writer.endNode();
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      String methodName = null;
      String declaringClassName = null;

      while ((methodName == null || declaringClassName == null) && reader.hasMoreChildren()) {
         reader.moveDown();
         if (reader.getNodeName().equals("name")) {
            methodName = reader.getValue();
         } else if (reader.getNodeName().equals("clazz")) {
            declaringClassName = reader.getValue();
         }

         reader.moveUp();
      }

      Class declaringClass = (Class)this.javaClassConverter.fromString(declaringClassName);

      try {
         return declaringClass.getDeclaredField(this.mapper.realMember(declaringClass, methodName));
      } catch (NoSuchFieldException e) {
         throw new ConversionException(e);
      }
   }
}
