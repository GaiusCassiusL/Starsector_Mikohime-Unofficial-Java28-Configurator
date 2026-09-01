package com.thoughtworks.xstream.core.util;

import java.util.HashMap;
import java.util.Map;

public final class Primitives {
   private static final Map BOX = new HashMap();
   private static final Map UNBOX = new HashMap();
   private static final Map NAMED_PRIMITIVE = new HashMap();
   private static final Map REPRESENTING_CHAR = new HashMap();

   public static Class box(Class type) {
      return (Class)BOX.get(type);
   }

   public static Class unbox(Class type) {
      return (Class)UNBOX.get(type);
   }

   public static boolean isBoxed(Class type) {
      return UNBOX.containsKey(type);
   }

   public static Class primitiveType(String name) {
      return (Class)NAMED_PRIMITIVE.get(name);
   }

   public static char representingChar(Class type) {
      Character ch = (Character)REPRESENTING_CHAR.get(type);
      return ch == null ? '\u0000' : ch;
   }

   static {
      Class[][] boxing = new Class[][]{
         {byte.class, Byte.class},
         {char.class, Character.class},
         {short.class, Short.class},
         {int.class, Integer.class},
         {long.class, Long.class},
         {float.class, Float.class},
         {double.class, Double.class},
         {boolean.class, Boolean.class},
         {void.class, Void.class}
      };
      Character[] representingChars = new Character[]{
         new Character('B'),
         new Character('C'),
         new Character('S'),
         new Character('I'),
         new Character('J'),
         new Character('F'),
         new Character('D'),
         new Character('Z'),
         null
      };

      for (int i = 0; i < boxing.length; i++) {
         Class primitiveType = boxing[i][0];
         Class boxedType = boxing[i][1];
         BOX.put(primitiveType, boxedType);
         UNBOX.put(boxedType, primitiveType);
         NAMED_PRIMITIVE.put(primitiveType.getName(), primitiveType);
         REPRESENTING_CHAR.put(primitiveType, representingChars[i]);
      }
   }
}
