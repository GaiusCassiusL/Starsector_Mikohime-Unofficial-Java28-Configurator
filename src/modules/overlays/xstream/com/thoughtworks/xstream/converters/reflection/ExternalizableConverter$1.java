package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.core.util.CustomObjectOutputStream;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.Map;

class ExternalizableConverter$1 implements CustomObjectOutputStream.StreamCallback {
   ExternalizableConverter$1(ExternalizableConverter this$0, HierarchicalStreamWriter var2, MarshallingContext var3) {
      this.this$0 = this$0;
      this.val$writer = var2;
      this.val$context = var3;
   }

   public void writeToStream(Object object) {
      if (object == null) {
         this.val$writer.startNode("null");
         this.val$writer.endNode();
      } else {
         ExtendedHierarchicalStreamWriterHelper.startNode(
            this.val$writer, ExternalizableConverter.access$000(this.this$0).serializedClass(object.getClass()), object.getClass()
         );
         this.val$context.convertAnother(object);
         this.val$writer.endNode();
      }
   }

   public void writeFieldsToStream(Map fields) {
      throw new UnsupportedOperationException();
   }

   public void defaultWriteObject() {
      throw new UnsupportedOperationException();
   }

   public void flush() {
      this.val$writer.flush();
   }

   public void close() {
      throw new UnsupportedOperationException("Objects are not allowed to call ObjectOutput.close() from writeExternal()");
   }
}
