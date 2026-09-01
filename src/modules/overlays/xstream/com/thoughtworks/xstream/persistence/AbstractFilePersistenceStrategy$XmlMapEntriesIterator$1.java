package com.thoughtworks.xstream.persistence;

import java.io.File;
import java.util.Map.Entry;

class AbstractFilePersistenceStrategy$XmlMapEntriesIterator$1 implements Entry {
   private final File file;
   private final Object key;

   AbstractFilePersistenceStrategy$XmlMapEntriesIterator$1(AbstractFilePersistenceStrategy.XmlMapEntriesIterator this$1) {
      this.this$1 = this$1;
      this.file = AbstractFilePersistenceStrategy.XmlMapEntriesIterator.access$202(
         this.this$1,
         AbstractFilePersistenceStrategy.XmlMapEntriesIterator.access$300(this.this$1)[AbstractFilePersistenceStrategy.XmlMapEntriesIterator.access$404(
            this.this$1
         )]
      );
      this.key = AbstractFilePersistenceStrategy.XmlMapEntriesIterator.access$500(this.this$1).extractKey(this.file.getName());
   }

   public Object getKey() {
      return this.key;
   }

   public Object getValue() {
      return AbstractFilePersistenceStrategy.access$600(AbstractFilePersistenceStrategy.XmlMapEntriesIterator.access$500(this.this$1), this.file);
   }

   public Object setValue(Object value) {
      return AbstractFilePersistenceStrategy.XmlMapEntriesIterator.access$500(this.this$1).put(this.key, value);
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof Entry)) {
         return false;
      }

      Object value = this.getValue();
      Entry e2 = (Entry)obj;
      Object key2 = e2.getKey();
      Object value2 = e2.getValue();
      return (this.key == null ? key2 == null : this.key.equals(key2)) && (value == null ? value2 == null : this.getValue().equals(e2.getValue()));
   }
}
