package org.apache.logging.log4j.core.util;

import java.lang.reflect.Array;

public class ArrayUtils {
   public static int getLength(final Object array) {
      return array == null ? 0 : Array.getLength(array);
   }

   public static boolean isEmpty(final byte[] array) {
      return getLength(array) == 0;
   }

   private static Object remove(final Object array, final int index) {
      int length = getLength(array);
      if (index >= 0 && index < length) {
         Object result = Array.newInstance(array.getClass().getComponentType(), length - 1);
         System.arraycopy(array, 0, result, 0, index);
         if (index < length - 1) {
            System.arraycopy(array, index + 1, result, index, length - index - 1);
         }

         return result;
      } else {
         throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
      }
   }

   public static <T> T[] remove(final T[] array, final int index) {
      return (T[])((Object[])remove(array, index));
   }
}
