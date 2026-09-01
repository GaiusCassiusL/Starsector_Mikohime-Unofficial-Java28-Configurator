package org.apache.log4j.helpers;

import java.io.IOException;
import java.io.Writer;
import org.apache.log4j.spi.ErrorHandler;

public class CountingQuietWriter extends QuietWriter {
   protected long count;

   public CountingQuietWriter(final Writer writer, final ErrorHandler eh) {
      super(writer, eh);
   }

   public long getCount() {
      return this.count;
   }

   public void setCount(final long count) {
      this.count = count;
   }

   @Override
   public void write(final String string) {
      try {
         this.out.write(string);
         this.count = this.count + string.length();
      } catch (IOException e) {
         this.errorHandler.error("Write failure.", e, 1);
      }
   }
}
