package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.CustomObjectInputStream;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import java.io.Externalizable;
import java.io.NotActiveException;
import java.io.ObjectInputValidation;
import java.util.Map;

class ExternalizableConverter$2 implements CustomObjectInputStream.StreamCallback {
   ExternalizableConverter$2(ExternalizableConverter this$0, HierarchicalStreamReader var2, UnmarshallingContext var3, Externalizable var4) {
      this.this$0 = this$0;
      this.val$reader = var2;
      this.val$context = var3;
      this.val$externalizable = var4;
   }

   public Object readFromStream() {
      this.val$reader.moveDown();
      Class type = HierarchicalStreams.readClassType(this.val$reader, ExternalizableConverter.access$000(this.this$0));
      Object streamItem = this.val$context.convertAnother(this.val$externalizable, type);
      this.val$reader.moveUp();
      return streamItem;
   }

   public Map readFieldsFromStream() {
      throw new UnsupportedOperationException();
   }

   public void defaultReadObject() {
      throw new UnsupportedOperationException();
   }

   public void registerValidation(ObjectInputValidation validation, int priority) throws NotActiveException {
      throw new NotActiveException("stream inactive");
   }

   public void close() {
      throw new UnsupportedOperationException("Objects are not allowed to call ObjectInput.close() from readExternal()");
   }
}
