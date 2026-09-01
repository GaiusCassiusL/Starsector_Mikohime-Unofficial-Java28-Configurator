package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.util.Fields;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.DynamicProxyMapper;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class DynamicProxyConverter implements Converter {
   private ClassLoaderReference classLoaderReference;
   private Mapper mapper;

   /** @deprecated */
   public DynamicProxyConverter(Mapper mapper) {
      this(mapper, DynamicProxyConverter.class.getClassLoader());
   }

   public DynamicProxyConverter(Mapper mapper, ClassLoaderReference classLoaderReference) {
      this.classLoaderReference = classLoaderReference;
      this.mapper = mapper;
   }

   /** @deprecated */
   public DynamicProxyConverter(Mapper mapper, ClassLoader classLoader) {
      this(mapper, new ClassLoaderReference(classLoader));
   }

   public boolean canConvert(Class type) {
      return type != null && (type.equals(DynamicProxyMapper.DynamicProxy.class) || Proxy.isProxyClass(type));
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      InvocationHandler invocationHandler = Proxy.getInvocationHandler(source);
      this.addInterfacesToXml(source, writer);
      writer.startNode("handler");
      String attributeName = this.mapper.aliasForSystemAttribute("class");
      if (attributeName != null) {
         writer.addAttribute(attributeName, this.mapper.serializedClass(invocationHandler.getClass()));
      }

      context.convertAnother(invocationHandler);
      writer.endNode();
   }

   private void addInterfacesToXml(Object source, HierarchicalStreamWriter writer) {
      Class[] interfaces = source.getClass().getInterfaces();

      for (int i = 0; i < interfaces.length; i++) {
         Class currentInterface = interfaces[i];
         writer.startNode("interface");
         writer.setValue(this.mapper.serializedClass(currentInterface));
         writer.endNode();
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      List interfaces = new ArrayList();
      InvocationHandler handler = null;
      Class handlerType = null;

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         String elementName = reader.getNodeName();
         if (elementName.equals("interface")) {
            interfaces.add(this.mapper.realClass(reader.getValue()));
         } else if (elementName.equals("handler")) {
            String attributeName = this.mapper.aliasForSystemAttribute("class");
            if (attributeName != null) {
               handlerType = this.mapper.realClass(reader.getAttribute(attributeName));
               break;
            }
         }

         reader.moveUp();
      }

      if (handlerType == null) {
         throw new ConversionException("No InvocationHandler specified for dynamic proxy");
      }

      Class[] interfacesAsArray = new Class[interfaces.size()];
      interfaces.toArray(interfacesAsArray);
      Object proxy = null;
      if (DynamicProxyConverter.Reflections.HANDLER != null) {
         proxy = Proxy.newProxyInstance(this.classLoaderReference.getReference(), interfacesAsArray, DynamicProxyConverter.Reflections.DUMMY);
      }

      handler = (InvocationHandler)context.convertAnother(proxy, handlerType);
      reader.moveUp();
      if (DynamicProxyConverter.Reflections.HANDLER != null) {
         Fields.write(DynamicProxyConverter.Reflections.HANDLER, proxy, handler);
      } else {
         proxy = Proxy.newProxyInstance(this.classLoaderReference.getReference(), interfacesAsArray, handler);
      }

      return proxy;
   }

   private static class Reflections {
      private static final Field HANDLER = Fields.locate(Proxy.class, InvocationHandler.class, false);
      private static final InvocationHandler DUMMY = new DynamicProxyConverter$Reflections$1();
   }
}
