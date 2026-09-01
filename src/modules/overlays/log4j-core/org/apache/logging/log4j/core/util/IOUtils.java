package org.apache.logging.log4j.core.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class IOUtils {
   private static final int DEFAULT_BUFFER_SIZE = 4096;
   public static final int EOF = -1;

   public static int copy(final Reader input, final Writer output) throws IOException {
      long count = copyLarge(input, output);
      return count > 2147483647L ? -1 : (int)count;
   }

   public static long copyLarge(final Reader input, final Writer output) throws IOException {
      return copyLarge(input, output, new char[4096]);
   }

   public static long copyLarge(final Reader input, final Writer output, final char[] buffer) throws IOException {
      long count = 0L;

      int n;
      while (-1 != (n = input.read(buffer))) {
         output.write(buffer, 0, n);
         count += n;
      }

      return count;
   }

   public static String toString(final Reader input) throws IOException {
      StringBuilderWriter sw = new StringBuilderWriter();
      copy(input, sw);
      return sw.toString();
   }
}
