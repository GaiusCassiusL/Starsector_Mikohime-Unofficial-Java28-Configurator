package com.thoughtworks.xstream.converters.reflection;

class SerializableConverter$UnserializableParentsReflectionProvider$1 implements ReflectionProvider.Visitor {
   SerializableConverter$UnserializableParentsReflectionProvider$1(
      SerializableConverter.UnserializableParentsReflectionProvider this$0, ReflectionProvider.Visitor var2
   ) {
      this.this$0 = this$0;
      this.val$visitor = var2;
   }

   public void visit(String name, Class type, Class definedIn, Object value) {
      if (!(SerializableConverter.class$java$io$Serializable == null
            ? (SerializableConverter.class$java$io$Serializable = SerializableConverter.class$("java.io.Serializable"))
            : SerializableConverter.class$java$io$Serializable)
         .isAssignableFrom(definedIn)) {
         this.val$visitor.visit(name, type, definedIn, value);
      }
   }
}
