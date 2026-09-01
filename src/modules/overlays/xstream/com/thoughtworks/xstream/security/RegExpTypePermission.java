package com.thoughtworks.xstream.security;

import java.util.regex.Pattern;

public class RegExpTypePermission implements TypePermission {
   private final Pattern[] patterns;

   public RegExpTypePermission(String[] patterns) {
      this(getPatterns(patterns));
   }

   public RegExpTypePermission(Pattern[] patterns) {
      this.patterns = patterns == null ? new Pattern[0] : patterns;
   }

   public boolean allows(Class type) {
      if (type != null) {
         String name = type.getName();

         for (int i = 0; i < this.patterns.length; i++) {
            if (this.patterns[i].matcher(name).matches()) {
               return true;
            }
         }
      }

      return false;
   }

   private static Pattern[] getPatterns(String[] patterns) {
      if (patterns == null) {
         return null;
      }

      Pattern[] array = new Pattern[patterns.length];

      for (int i = 0; i < array.length; i++) {
         array[i] = Pattern.compile(patterns[i]);
      }

      return array;
   }
}
