package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.path.Path;
import java.util.Iterator;

class AbstractReferenceMarshaller$1 implements ReferencingMarshallingContext {
   AbstractReferenceMarshaller$1(AbstractReferenceMarshaller this$0, Object var2, Path var3) {
      this.this$0 = this$0;
      this.val$newReferenceKey = var2;
      this.val$currentPath = var3;
   }

   public void put(Object key, Object value) {
      this.this$0.put(key, value);
   }

   public Iterator keys() {
      return this.this$0.keys();
   }

   public Object get(Object key) {
      return this.this$0.get(key);
   }

   public void convertAnother(Object nextItem, Converter converter) {
      this.this$0.convertAnother(nextItem, converter);
   }

   public void convertAnother(Object nextItem) {
      this.this$0.convertAnother(nextItem);
   }

   public void replace(Object original, Object replacement) {
      AbstractReferenceMarshaller.access$000(this.this$0)
         .associateId(replacement, new AbstractReferenceMarshaller.Id(this.val$newReferenceKey, this.val$currentPath));
   }

   public Object lookupReference(Object item) {
      AbstractReferenceMarshaller.Id id = (AbstractReferenceMarshaller.Id)AbstractReferenceMarshaller.access$000(this.this$0).lookupId(item);
      return id.getItem();
   }

   /** @deprecated */
   public Path currentPath() {
      return AbstractReferenceMarshaller.access$100(this.this$0).getPath();
   }

   public void registerImplicit(Object item) {
      if (AbstractReferenceMarshaller.access$200(this.this$0).containsId(item)) {
         throw new AbstractReferenceMarshaller.ReferencedImplicitElementException(item, this.val$currentPath);
      }

      AbstractReferenceMarshaller.access$200(this.this$0).associateId(item, this.val$newReferenceKey);
   }
}
