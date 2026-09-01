package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.ReaderWrapper;
import java.io.IOException;
import java.io.InputStream;

class StaxDriver$2 extends ReaderWrapper {
   StaxDriver$2(StaxDriver this$0, HierarchicalStreamReader reader, InputStream var3) {
      super(reader);
      this.this$0 = this$0;
      this.val$stream = var3;
   }

   public void close() {
      super.close();

      try {
         this.val$stream.close();
      } catch (IOException var2) {
      }
   }
}
