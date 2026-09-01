package com.thoughtworks.xstream.converters.javabean;

public interface JavaBeanProvider {
   Object newInstance(Class var1);

   void visitSerializableProperties(Object var1, JavaBeanProvider.Visitor var2);

   void writeProperty(Object var1, String var2, Object var3);

   Class getPropertyType(Object var1, String var2);

   boolean propertyDefinedInClass(String var1, Class var2);

   boolean canInstantiate(Class var1);

   interface Visitor {
      boolean shouldVisit(String var1, Class var2);

      void visit(String var1, Class var2, Class var3, Object var4);
   }
}
