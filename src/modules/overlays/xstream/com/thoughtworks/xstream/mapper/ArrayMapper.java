package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.core.util.Primitives;

public class ArrayMapper extends MapperWrapper {
   public ArrayMapper(Mapper wrapped) {
      super(wrapped);
   }

   public String serializedClass(Class type) {
      StringBuffer arraySuffix = new StringBuffer();

      String name;
      for (name = null; type.isArray(); name = null) {
         name = super.serializedClass(type);
         if (!type.getName().equals(name)) {
            break;
         }

         type = type.getComponentType();
         arraySuffix.append("-array");
      }

      if (name == null) {
         name = this.boxedTypeName(type);
      }

      if (name == null) {
         name = super.serializedClass(type);
      }

      return arraySuffix.length() > 0 ? name + arraySuffix : name;
   }

   public Class realClass(String elementName) {
      int dimensions;
      for (dimensions = 0; elementName.endsWith("-array"); dimensions++) {
         elementName = elementName.substring(0, elementName.length() - 6);
      }

      if (dimensions <= 0) {
         return super.realClass(elementName);
      }

      Class componentType = Primitives.primitiveType(elementName);
      if (componentType == null) {
         componentType = super.realClass(elementName);
      }

      while (componentType.isArray()) {
         componentType = componentType.getComponentType();
         dimensions++;
      }

      return super.realClass(this.arrayType(dimensions, componentType));
   }

   private String arrayType(int dimensions, Class componentType) {
      StringBuffer className = new StringBuffer();

      for (int i = 0; i < dimensions; i++) {
         className.append('[');
      }

      if (componentType.isPrimitive()) {
         className.append(Primitives.representingChar(componentType));
         return className.toString();
      } else {
         className.append('L').append(componentType.getName()).append(';');
         return className.toString();
      }
   }

   private String boxedTypeName(Class type) {
      return Primitives.isBoxed(type) ? type.getName() : null;
   }
}
