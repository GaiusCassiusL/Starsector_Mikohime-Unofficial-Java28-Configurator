package com.thoughtworks.xstream.converters.reflection;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

class PureJavaReflectionProvider$1 extends ObjectInputStream {
   PureJavaReflectionProvider$1(PureJavaReflectionProvider this$0, InputStream x0, Class var3) {
      super(x0);
      this.this$0 = this$0;
      this.val$type = var3;
   }

   protected Class resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
      return Class.forName(desc.getName(), false, this.val$type.getClassLoader());
   }
}
