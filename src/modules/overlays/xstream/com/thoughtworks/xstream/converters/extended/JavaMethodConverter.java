package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class JavaMethodConverter implements Converter {
   private final SingleValueConverter javaClassConverter;

   public JavaMethodConverter(ClassLoaderReference classLoaderReference) {
      this(new JavaClassConverter(classLoaderReference));
   }

   /** @deprecated */
   public JavaMethodConverter(ClassLoader classLoader) {
      this(new ClassLoaderReference(classLoader));
   }

   protected JavaMethodConverter(SingleValueConverter javaClassConverter) {
      this.javaClassConverter = javaClassConverter;
   }

   public boolean canConvert(Class type) {
      return type == Method.class || type == Constructor.class;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      if (source instanceof Method) {
         Method method = (Method)source;
         String declaringClassName = this.javaClassConverter.toString(method.getDeclaringClass());
         this.marshalMethod(writer, declaringClassName, method.getName(), method.getParameterTypes());
      } else {
         Constructor method = (Constructor)source;
         String declaringClassName = this.javaClassConverter.toString(method.getDeclaringClass());
         this.marshalMethod(writer, declaringClassName, null, method.getParameterTypes());
      }
   }

   private void marshalMethod(HierarchicalStreamWriter writer, String declaringClassName, String methodName, Class[] parameterTypes) {
      writer.startNode("class");
      writer.setValue(declaringClassName);
      writer.endNode();
      if (methodName != null) {
         writer.startNode("name");
         writer.setValue(methodName);
         writer.endNode();
      }

      writer.startNode("parameter-types");

      for (int i = 0; i < parameterTypes.length; i++) {
         writer.startNode("class");
         writer.setValue(this.javaClassConverter.toString(parameterTypes[i]));
         writer.endNode();
      }

      writer.endNode();
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      try {
         boolean isMethodNotConstructor = context.getRequiredType().equals(Method.class);
         reader.moveDown();
         String declaringClassName = reader.getValue();
         Class declaringClass = (Class)this.javaClassConverter.fromString(declaringClassName);
         reader.moveUp();
         String methodName = null;
         if (isMethodNotConstructor) {
            reader.moveDown();
            methodName = reader.getValue();
            reader.moveUp();
         }

         reader.moveDown();
         List parameterTypeList = new ArrayList();

         while (reader.hasMoreChildren()) {
            reader.moveDown();
            String parameterTypeName = reader.getValue();
            parameterTypeList.add(this.javaClassConverter.fromString(parameterTypeName));
            reader.moveUp();
         }

         Class[] parameterTypes = parameterTypeList.toArray(new Class[parameterTypeList.size()]);
         reader.moveUp();
         return isMethodNotConstructor ? declaringClass.getDeclaredMethod(methodName, parameterTypes) : declaringClass.getDeclaredConstructor(parameterTypes);
      } catch (NoSuchMethodException e) {
         throw new ConversionException(e);
      }
   }
}
