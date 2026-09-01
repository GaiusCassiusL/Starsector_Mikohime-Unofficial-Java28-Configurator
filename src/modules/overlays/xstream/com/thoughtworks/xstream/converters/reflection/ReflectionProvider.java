package com.thoughtworks.xstream.converters.reflection;

import java.lang.reflect.Field;

public interface ReflectionProvider {
   Object newInstance(Class var1);

   void visitSerializableFields(Object var1, ReflectionProvider.Visitor var2);

   void writeField(Object var1, String var2, Object var3, Class var4);

   Class getFieldType(Object var1, String var2, Class var3);

   /** @deprecated */
   boolean fieldDefinedInClass(String var1, Class var2);

   Field getField(Class var1, String var2);

   Field getFieldOrNull(Class var1, String var2);

   interface Visitor {
      void visit(String var1, Class var2, Class var3, Object var4);
   }
}
