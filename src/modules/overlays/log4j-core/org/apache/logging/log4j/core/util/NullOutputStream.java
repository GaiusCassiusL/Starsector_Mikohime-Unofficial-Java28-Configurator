package org.apache.logging.log4j.core.util;

import java.io.IOException;
import java.io.OutputStream;

public final class NullOutputStream extends OutputStream {
   private static final NullOutputStream INSTANCE = new NullOutputStream();

   public static NullOutputStream getInstance() {
      return INSTANCE;
   }

   private NullOutputStream() {
   }

   @Override
   public void write(final byte[] b, final int off, final int len) {
   }

   @Override
   public void write(final int b) {
   }

   @Override
   public void write(final byte[] b) throws IOException {
   }
}
