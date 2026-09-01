package com.thoughtworks.xstream.converters.reflection;

import java.io.InvalidObjectException;
import java.io.ObjectInputValidation;

class SerializableConverter$2$1 implements Runnable {
   SerializableConverter$2$1(SerializableConverter$2 this$1, ObjectInputValidation var2) {
      this.this$1 = this$1;
      this.val$validation = var2;
   }

   public void run() {
      try {
         this.val$validation.validateObject();
      } catch (InvalidObjectException e) {
         throw new ObjectAccessException("Cannot validate object", e);
      }
   }
}
