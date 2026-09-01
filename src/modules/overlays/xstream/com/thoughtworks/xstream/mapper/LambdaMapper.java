package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.core.util.Types;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class LambdaMapper extends MapperWrapper {
   public LambdaMapper(Mapper wrapped) {
      super(wrapped);
   }

   @Override
   public String serializedClass(Class type) {
      Class<?> replacement = null;
      if (Types.isLambdaType(type)) {
         if (Serializable.class.isAssignableFrom(type)) {
            Class<?>[] interfaces = type.getInterfaces();
            if (interfaces.length > 1) {
               for (int i = 0; replacement == null && i < interfaces.length; i++) {
                  Class<?> iface = interfaces[i];

                  for (Method method : iface.getMethods()) {
                     if (!method.isDefault() && !Modifier.isStatic(method.getModifiers())) {
                        replacement = iface;
                        break;
                     }
                  }
               }
            } else {
               replacement = interfaces[0];
            }
         } else {
            replacement = Mapper.Null.class;
         }
      }

      return super.serializedClass(replacement == null ? type : replacement);
   }
}
