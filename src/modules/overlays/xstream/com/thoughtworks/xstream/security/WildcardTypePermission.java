package com.thoughtworks.xstream.security;

public class WildcardTypePermission extends RegExpTypePermission {
   public WildcardTypePermission(String[] patterns) {
      this(false, patterns);
   }

   public WildcardTypePermission(boolean allowAnonymous, String[] patterns) {
      super(getRegExpPatterns(patterns, allowAnonymous));
   }

   private static String[] getRegExpPatterns(String[] wildcards, boolean allowAnonymous) {
      if (wildcards == null) {
         return null;
      }

      String[] regexps = new String[wildcards.length];

      for (int i = 0; i < wildcards.length; i++) {
         String wildcardExpression = wildcards[i];
         StringBuffer result = new StringBuffer(wildcardExpression.length() * 2);
         result.append("(?u)");
         int length = wildcardExpression.length();

         for (int j = 0; j < length; j++) {
            char ch = wildcardExpression.charAt(j);
            switch (ch) {
               case '$':
               case '(':
               case ')':
               case '+':
               case '.':
               case '[':
               case '\\':
               case ']':
               case '^':
               case '|':
                  result.append('\\').append(ch);
                  break;
               case '*':
                  if (j + 1 < length && wildcardExpression.charAt(j + 1) == '*') {
                     result.append(allowAnonymous ? "[\\P{C}]*" : "[\\P{C}&&[^$]]*(?:\\$[^0-9$][\\P{C}&&[^.$]]*)*");
                     j++;
                  } else {
                     result.append(allowAnonymous ? "[\\P{C}&&[^.]]*" : "[\\P{C}&&[^.$]]*(?:\\$[^0-9$][\\P{C}&&[^.$]]*)*");
                  }
                  break;
               case '?':
                  result.append('.');
                  break;
               default:
                  result.append(ch);
            }
         }

         regexps[i] = result.toString();
      }

      return regexps;
   }
}
