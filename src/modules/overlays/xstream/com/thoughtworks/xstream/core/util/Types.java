package com.thoughtworks.xstream.core.util;

import java.util.regex.Pattern;

public class Types {
   private static final Pattern lambdaPattern = Pattern.compile(".*\\$\\$Lambda(?:\\$[0-9]+|)/.*");

   public static final boolean isLambdaType(Class<?> type) {
      if (type != null && type.isSynthetic()) {
         String typeName = type.getSimpleName();
         if (typeName.length() == 0) {
            typeName = type.getName();
         }

         return lambdaPattern.matcher(typeName).matches();
      } else {
         return false;
      }
   }
}
