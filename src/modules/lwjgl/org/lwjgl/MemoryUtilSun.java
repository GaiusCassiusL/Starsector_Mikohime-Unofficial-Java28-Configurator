package org.lwjgl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import sun.misc.Unsafe;

final class MemoryUtilSun {
   private MemoryUtilSun() {
   }

   private static class AccessorUnsafe implements MemoryUtil.Accessor {
      private final Unsafe unsafe;
      private final long address;

      AccessorUnsafe() {
         try {
            this.unsafe = getUnsafeInstance();
            this.address = this.unsafe.objectFieldOffset(MemoryUtil.getAddressField());
         } catch (Exception e) {
            throw new UnsupportedOperationException(e);
         }
      }

      @Override
      public long getAddress(Buffer buffer) {
         return this.unsafe.getLong(buffer, this.address);
      }

      private static Unsafe getUnsafeInstance() {
         Field[] fields = Unsafe.class.getDeclaredFields();

         for (Field field : fields) {
            if (field.getType().equals(Unsafe.class)) {
               int modifiers = field.getModifiers();
               if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                  field.setAccessible(true);

                  try {
                     return (Unsafe)field.get(null);
                  } catch (IllegalAccessException var7) {
                     break;
                  }
               }
            }
         }

         throw new UnsupportedOperationException();
      }
   }
}
