package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.ErrorWritingException;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;

class ThreadSafePropertyEditor$1 implements Pool.Factory {
   ThreadSafePropertyEditor$1(ThreadSafePropertyEditor this$0) {
      this.this$0 = this$0;
   }

   public Object newInstance() {
      ErrorWritingException ex = null;

      try {
         return ThreadSafePropertyEditor.access$000(this.this$0).newInstance();
      } catch (InstantiationException e) {
         ex = new ConversionException("Faild to call default constructor", e);
      } catch (IllegalAccessException e) {
         ex = new ObjectAccessException("Cannot call default constructor", e);
      }

      ex.add("construction-type", ThreadSafePropertyEditor.access$000(this.this$0).getName());
      throw ex;
   }
}
