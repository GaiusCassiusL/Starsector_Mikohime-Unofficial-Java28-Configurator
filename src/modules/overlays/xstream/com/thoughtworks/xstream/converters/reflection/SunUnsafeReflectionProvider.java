package com.thoughtworks.xstream.converters.reflection;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

public class SunUnsafeReflectionProvider extends SunLimitedUnsafeReflectionProvider {
   private transient Map fieldOffsetCache;

   public SunUnsafeReflectionProvider() {
   }

   public SunUnsafeReflectionProvider(FieldDictionary dic) {
      super(dic);
   }

   public void writeField(Object object, String fieldName, Object value, Class definedIn) {
      this.write(this.fieldDictionary.field(object.getClass(), fieldName, definedIn), object, value);
   }

   private void write(Field field, Object object, Object value) {
      if (exception != null) {
         ObjectAccessException ex = new ObjectAccessException("Cannot set field", exception);
         ex.add("field", object.getClass() + "." + field.getName());
         throw ex;
      }

      try {
         long offset = this.getFieldOffset(field);
         Class type = field.getType();
         if (type.isPrimitive()) {
            if (type.equals(int.class)) {
               unsafe.putInt(object, offset, (Integer)value);
            } else if (type.equals(long.class)) {
               unsafe.putLong(object, offset, (Long)value);
            } else if (type.equals(short.class)) {
               unsafe.putShort(object, offset, (Short)value);
            } else if (type.equals(char.class)) {
               unsafe.putChar(object, offset, (Character)value);
            } else if (type.equals(byte.class)) {
               unsafe.putByte(object, offset, (Byte)value);
            } else if (type.equals(float.class)) {
               unsafe.putFloat(object, offset, (Float)value);
            } else if (type.equals(double.class)) {
               unsafe.putDouble(object, offset, (Double)value);
            } else {
               if (!type.equals(boolean.class)) {
                  ObjectAccessException ex = new ObjectAccessException("Cannot set field of unknown type", exception);
                  ex.add("field", object.getClass() + "." + field.getName());
                  ex.add("unknown-type", type.getName());
                  throw ex;
               }

               unsafe.putBoolean(object, offset, (Boolean)value);
            }
         } else {
            unsafe.putObject(object, offset, value);
         }
      } catch (IllegalArgumentException e) {
         ObjectAccessException ex = new ObjectAccessException("Cannot set field", e);
         ex.add("field", object.getClass() + "." + field.getName());
         throw ex;
      }
   }

   private synchronized long getFieldOffset(Field f) {
      Long l = (Long)this.fieldOffsetCache.get(f);
      if (l == null) {
         l = new Long(unsafe.objectFieldOffset(f));
         this.fieldOffsetCache.put(f, l);
      }

      return l;
   }

   private Object readResolve() {
      this.init();
      return this;
   }

   protected void init() {
      super.init();
      this.fieldOffsetCache = new WeakHashMap();
   }
}
