package com.thoughtworks.xstream.core.util;

import java.beans.PropertyEditor;

public class ThreadSafePropertyEditor {
   private final Class editorType;
   private final Pool pool;

   public ThreadSafePropertyEditor(Class type, int initialPoolSize, int maxPoolSize) {
      if (!PropertyEditor.class.isAssignableFrom(type)) {
         throw new IllegalArgumentException(type.getName() + " is not a " + PropertyEditor.class.getName());
      }

      this.editorType = type;
      this.pool = new Pool(initialPoolSize, maxPoolSize, new ThreadSafePropertyEditor$1(this));
   }

   public String getAsText(Object object) {
      PropertyEditor editor = this.fetchFromPool();

      try {
         editor.setValue(object);
         return editor.getAsText();
      } finally {
         this.pool.putInPool(editor);
      }
   }

   public Object setAsText(String str) {
      PropertyEditor editor = this.fetchFromPool();

      try {
         editor.setAsText(str);
         return editor.getValue();
      } finally {
         this.pool.putInPool(editor);
      }
   }

   private PropertyEditor fetchFromPool() {
      return (PropertyEditor)this.pool.fetchFromPool();
   }
}
