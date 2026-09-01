package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.CGLIBMapper;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.NoOp;

public class CGLIBEnhancedConverter extends SerializableConverter {
   private static String DEFAULT_NAMING_MARKER = "$$EnhancerByCGLIB$$";
   private static String CALLBACK_MARKER = "CGLIB$CALLBACK_";
   private transient Map fieldCache = new HashMap();

   public CGLIBEnhancedConverter(Mapper mapper, ReflectionProvider reflectionProvider, ClassLoaderReference classLoaderReference) {
      super(mapper, new CGLIBEnhancedConverter.CGLIBFilteringReflectionProvider(reflectionProvider), classLoaderReference);
   }

   /** @deprecated */
   public CGLIBEnhancedConverter(Mapper mapper, ReflectionProvider reflectionProvider, ClassLoader classLoader) {
      super(mapper, new CGLIBEnhancedConverter.CGLIBFilteringReflectionProvider(reflectionProvider), classLoader);
   }

   /** @deprecated */
   public CGLIBEnhancedConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
      this(mapper, new CGLIBEnhancedConverter.CGLIBFilteringReflectionProvider(reflectionProvider), CGLIBEnhancedConverter.class.getClassLoader());
   }

   public boolean canConvert(Class type) {
      return type != null && Enhancer.isEnhanced(type) && type.getName().indexOf(DEFAULT_NAMING_MARKER) > 0 || type == CGLIBMapper.Marker.class;
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Class type = source.getClass();
      boolean hasFactory = Factory.class.isAssignableFrom(type);
      ExtendedHierarchicalStreamWriterHelper.startNode(writer, "type", type);
      context.convertAnother(type.getSuperclass());
      writer.endNode();
      writer.startNode("interfaces");
      Class[] interfaces = type.getInterfaces();

      for (int i = 0; i < interfaces.length; i++) {
         if (interfaces[i] != Factory.class) {
            ExtendedHierarchicalStreamWriterHelper.startNode(writer, this.mapper.serializedClass(interfaces[i].getClass()), interfaces[i].getClass());
            context.convertAnother(interfaces[i]);
            writer.endNode();
         }
      }

      writer.endNode();
      writer.startNode("hasFactory");
      writer.setValue(String.valueOf(hasFactory));
      writer.endNode();
      Map callbackIndexMap = null;
      Callback[] callbacks = hasFactory ? ((Factory)source).getCallbacks() : this.getCallbacks(source);
      if (callbacks.length > 1) {
         if (!hasFactory) {
            ConversionException exception = new ConversionException("Cannot handle CGLIB enhanced proxies without factory that have multiple callbacks");
            exception.add("proxy-superclass", type.getSuperclass().getName());
            exception.add("number-of-callbacks", String.valueOf(callbacks.length));
            throw exception;
         }

         callbackIndexMap = this.createCallbackIndexMap((Factory)source);
         writer.startNode("callbacks");
         writer.startNode("mapping");
         context.convertAnother(callbackIndexMap);
         writer.endNode();
      }

      boolean hasInterceptor = false;

      for (int i = 0; i < callbacks.length; i++) {
         Callback callback = callbacks[i];
         if (callback == null) {
            String name = this.mapper.serializedClass(null);
            writer.startNode(name);
            writer.endNode();
         } else {
            hasInterceptor = hasInterceptor || MethodInterceptor.class.isAssignableFrom(callback.getClass());
            ExtendedHierarchicalStreamWriterHelper.startNode(writer, this.mapper.serializedClass(callback.getClass()), callback.getClass());
            context.convertAnother(callback);
            writer.endNode();
         }
      }

      if (callbacks.length > 1) {
         writer.endNode();
      }

      try {
         Field field = type.getDeclaredField("serialVersionUID");
         if (!field.isAccessible()) {
            field.setAccessible(true);
         }

         long serialVersionUID = field.getLong(null);
         ExtendedHierarchicalStreamWriterHelper.startNode(writer, "serialVersionUID", String.class);
         writer.setValue(String.valueOf(serialVersionUID));
         writer.endNode();
      } catch (NoSuchFieldException var13) {
      } catch (IllegalAccessException e) {
         ObjectAccessException exception = new ObjectAccessException("Cannot access field", e);
         exception.add("field", type.getName() + ".serialVersionUID");
         throw exception;
      }

      if (hasInterceptor) {
         writer.startNode("instance");
         super.doMarshalConditionally(source, writer, context);
         writer.endNode();
      }
   }

   private Callback[] getCallbacks(Object source) {
      Class type = source.getClass();
      List fields = (List)this.fieldCache.get(type.getName());
      if (fields == null) {
         fields = new ArrayList();
         this.fieldCache.put(type.getName(), fields);
         int i = 0;

         while (true) {
            try {
               Field field = type.getDeclaredField(CALLBACK_MARKER + i);
               if (!field.isAccessible()) {
                  field.setAccessible(true);
               }

               fields.add(field);
            } catch (NoSuchFieldException e) {
               break;
            }

            i++;
         }
      }

      List list = new ArrayList();

      for (int i = 0; i < fields.size(); i++) {
         try {
            Field field = (Field)fields.get(i);
            Object callback = field.get(source);
            list.add(callback);
         } catch (IllegalAccessException e) {
            ObjectAccessException exception = new ObjectAccessException("Cannot access field", e);
            exception.add("field", type.getName() + "." + CALLBACK_MARKER + i);
            throw exception;
         }
      }

      return list.toArray(new Callback[list.size()]);
   }

   private Map createCallbackIndexMap(Factory source) {
      Callback[] originalCallbacks = source.getCallbacks();
      Callback[] reverseEngineeringCallbacks = new Callback[originalCallbacks.length];
      Map callbackIndexMap = new HashMap();
      int idxNoOp = -1;

      for (int i = 0; i < originalCallbacks.length; i++) {
         Callback callback = originalCallbacks[i];
         if (callback == null) {
            reverseEngineeringCallbacks[i] = null;
         } else if (NoOp.class.isAssignableFrom(callback.getClass())) {
            reverseEngineeringCallbacks[i] = NoOp.INSTANCE;
            idxNoOp = i;
         } else {
            reverseEngineeringCallbacks[i] = this.createReverseEngineeredCallbackOfProperType(callback, i, callbackIndexMap);
         }
      }

      try {
         source.setCallbacks(reverseEngineeringCallbacks);
         Set interfaces = new HashSet();
         Set methods = new HashSet();
         Class type = source.getClass();

         do {
            methods.addAll(Arrays.asList(type.getDeclaredMethods()));
            methods.addAll(Arrays.asList(type.getMethods()));
            Class[] implementedInterfaces = type.getInterfaces();
            interfaces.addAll(Arrays.asList(implementedInterfaces));
            type = type.getSuperclass();
         } while (type != null);

         Iterator iterator = interfaces.iterator();

         while (iterator.hasNext()) {
            type = (Class)iterator.next();
            methods.addAll(Arrays.asList(type.getDeclaredMethods()));
         }

         iterator = methods.iterator();

         while (iterator.hasNext()) {
            Method method = (Method)iterator.next();
            if (!method.isAccessible()) {
               method.setAccessible(true);
            }

            if (!Factory.class.isAssignableFrom(method.getDeclaringClass()) && (method.getModifiers() & 24) <= 0) {
               Class[] parameterTypes = method.getParameterTypes();
               Method calledMethod = method;

               try {
                  if ((method.getModifiers() & 1024) > 0) {
                     calledMethod = source.getClass().getMethod(method.getName(), method.getParameterTypes());
                  }

                  callbackIndexMap.put(null, method);
                  calledMethod.invoke(source, parameterTypes == null ? (Object[])null : this.createNullArguments(parameterTypes));
               } catch (IllegalAccessException e) {
                  ObjectAccessException exception = new ObjectAccessException("Cannot access method", e);
                  exception.add("method", calledMethod.toString());
                  throw exception;
               } catch (InvocationTargetException var21) {
               } catch (NoSuchMethodException e) {
                  ConversionException exception = new ConversionException("CGLIB enhanced proxies wit abstract nethod that has not been implemented");
                  exception.add("proxy-superclass", type.getSuperclass().getName());
                  exception.add("method", method.toString());
                  throw exception;
               }

               if (callbackIndexMap.containsKey(method)) {
                  iterator.remove();
               }
            } else {
               iterator.remove();
            }
         }

         if (idxNoOp >= 0) {
            Integer idx = new Integer(idxNoOp);
            Iterator iter = methods.iterator();

            while (iter.hasNext()) {
               callbackIndexMap.put(iter.next(), idx);
            }
         }
      } finally {
         source.setCallbacks(originalCallbacks);
      }

      callbackIndexMap.remove(null);
      return callbackIndexMap;
   }

   private Object[] createNullArguments(Class[] parameterTypes) {
      Object[] arguments = new Object[parameterTypes.length];

      for (int i = 0; i < arguments.length; i++) {
         Class type = parameterTypes[i];
         if (type.isPrimitive()) {
            if (type == byte.class) {
               arguments[i] = new Byte((byte)0);
            } else if (type == short.class) {
               arguments[i] = new Short((short)0);
            } else if (type == int.class) {
               arguments[i] = new Integer(0);
            } else if (type == long.class) {
               arguments[i] = new Long(0L);
            } else if (type == float.class) {
               arguments[i] = new Float(0.0F);
            } else if (type == double.class) {
               arguments[i] = new Double(0.0);
            } else if (type == char.class) {
               arguments[i] = new Character('\u0000');
            } else {
               arguments[i] = Boolean.FALSE;
            }
         }
      }

      return arguments;
   }

   private Callback createReverseEngineeredCallbackOfProperType(Callback callback, int index, Map callbackIndexMap) {
      Class iface = null;
      Class[] interfaces = callback.getClass().getInterfaces();
      int i = 0;

      while (true) {
         label38: {
            if (i < interfaces.length) {
               if (!Callback.class.isAssignableFrom(interfaces[i])) {
                  break label38;
               }

               iface = interfaces[i];
               if (iface == Callback.class) {
                  ConversionException exception = new ConversionException("Cannot handle CGLIB callback");
                  exception.add("CGLIB-callback-type", callback.getClass().getName());
                  throw exception;
               }

               interfaces = iface.getInterfaces();
               if (!Arrays.asList(interfaces).contains(Callback.class)) {
                  i = -1;
                  break label38;
               }
            }

            return (Callback)Proxy.newProxyInstance(
               iface.getClassLoader(), new Class[]{iface}, new CGLIBEnhancedConverter.ReverseEngineeringInvocationHandler(index, callbackIndexMap)
            );
         }

         i++;
      }
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Enhancer enhancer = new Enhancer();
      reader.moveDown();
      enhancer.setSuperclass((Class)context.convertAnother(null, Class.class));
      reader.moveUp();
      reader.moveDown();
      List interfaces = new ArrayList();

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         interfaces.add(context.convertAnother(null, this.mapper.realClass(reader.getNodeName())));
         reader.moveUp();
      }

      enhancer.setInterfaces(interfaces.toArray(new Class[interfaces.size()]));
      reader.moveUp();
      reader.moveDown();
      boolean useFactory = Boolean.valueOf(reader.getValue());
      enhancer.setUseFactory(useFactory);
      reader.moveUp();
      List callbacksToEnhance = new ArrayList();
      List callbacks = new ArrayList();
      Map callbackIndexMap = null;
      reader.moveDown();
      if ("callbacks".equals(reader.getNodeName())) {
         reader.moveDown();
         callbackIndexMap = (Map)context.convertAnother(null, HashMap.class);
         reader.moveUp();

         while (reader.hasMoreChildren()) {
            reader.moveDown();
            this.readCallback(reader, context, callbacksToEnhance, callbacks);
            reader.moveUp();
         }
      } else {
         this.readCallback(reader, context, callbacksToEnhance, callbacks);
      }

      enhancer.setCallbacks(callbacksToEnhance.toArray(new Callback[callbacksToEnhance.size()]));
      if (callbackIndexMap != null) {
         enhancer.setCallbackFilter(new CGLIBEnhancedConverter.ReverseEngineeredCallbackFilter(callbackIndexMap));
      }

      reader.moveUp();
      Object result = null;

      while (reader.hasMoreChildren()) {
         reader.moveDown();
         if (reader.getNodeName().equals("serialVersionUID")) {
            enhancer.setSerialVersionUID(Long.valueOf(reader.getValue()));
         } else if (reader.getNodeName().equals("instance")) {
            result = this.create(enhancer, callbacks, useFactory);
            super.doUnmarshalConditionally(result, reader, context);
         }

         reader.moveUp();
      }

      if (result == null) {
         result = this.create(enhancer, callbacks, useFactory);
      }

      return this.serializationMembers.callReadResolve(result);
   }

   private void readCallback(HierarchicalStreamReader reader, UnmarshallingContext context, List callbacksToEnhance, List callbacks) {
      Callback callback = (Callback)context.convertAnother(null, this.mapper.realClass(reader.getNodeName()));
      callbacks.add(callback);
      if (callback == null) {
         callbacksToEnhance.add(NoOp.INSTANCE);
      } else {
         callbacksToEnhance.add(callback);
      }
   }

   private Object create(Enhancer enhancer, List callbacks, boolean useFactory) {
      Object result = enhancer.create();
      if (useFactory) {
         ((Factory)result).setCallbacks(callbacks.toArray(new Callback[callbacks.size()]));
      }

      return result;
   }

   protected List hierarchyFor(Class type) {
      List typeHierarchy = super.hierarchyFor(type);
      typeHierarchy.remove(typeHierarchy.size() - 1);
      return typeHierarchy;
   }

   protected Object readResolve() {
      super.readResolve();
      this.fieldCache = new HashMap();
      return this;
   }

   private static class CGLIBFilteringReflectionProvider extends ReflectionProviderWrapper {
      public CGLIBFilteringReflectionProvider(ReflectionProvider reflectionProvider) {
         super(reflectionProvider);
      }

      public void visitSerializableFields(Object object, ReflectionProvider.Visitor visitor) {
         this.wrapped.visitSerializableFields(object, new CGLIBEnhancedConverter$CGLIBFilteringReflectionProvider$1(this, visitor));
      }
   }

   private static class ReverseEngineeredCallbackFilter implements CallbackFilter {
      private final Map callbackIndexMap;

      public ReverseEngineeredCallbackFilter(Map callbackIndexMap) {
         this.callbackIndexMap = callbackIndexMap;
      }

      public int accept(Method method) {
         if (!this.callbackIndexMap.containsKey(method)) {
            ConversionException exception = new ConversionException("CGLIB callback not detected in reverse engineering");
            exception.add("CGLIB-callback", method.toString());
            throw exception;
         } else {
            return (Integer)this.callbackIndexMap.get(method);
         }
      }
   }

   private static final class ReverseEngineeringInvocationHandler implements InvocationHandler {
      private final Integer index;
      private final Map indexMap;

      public ReverseEngineeringInvocationHandler(int index, Map indexMap) {
         this.indexMap = indexMap;
         this.index = new Integer(index);
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         this.indexMap.put(this.indexMap.get(null), this.index);
         return null;
      }
   }
}
