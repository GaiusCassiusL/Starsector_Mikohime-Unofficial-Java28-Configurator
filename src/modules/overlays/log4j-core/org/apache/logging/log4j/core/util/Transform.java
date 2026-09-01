package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.util.Strings;

public final class Transform {
   private static final String CDATA_START = "<![CDATA[";
   private static final String CDATA_END = "]]>";
   private static final String CDATA_PSEUDO_END = "]]&gt;";
   private static final String CDATA_EMBEDED_END = "]]>]]&gt;<![CDATA[";
   private static final int CDATA_END_LEN = "]]>".length();

   private Transform() {
   }

   public static String escapeHtmlTags(final String input) {
      if (!Strings.isEmpty(input) && (input.indexOf(34) != -1 || input.indexOf(38) != -1 || input.indexOf(60) != -1 || input.indexOf(62) != -1)) {
         StringBuilder buf = new StringBuilder(input.length() + 6);
         int len = input.length();

         for (int i = 0; i < len; i++) {
            char ch = input.charAt(i);
            if (ch > '>') {
               buf.append(ch);
            } else if (ch == '<') {
               buf.append("&lt;");
            } else if (ch == '>') {
               buf.append("&gt;");
            } else if (ch == '&') {
               buf.append("&amp;");
            } else if (ch == '"') {
               buf.append("&quot;");
            } else {
               buf.append(ch);
            }
         }

         return buf.toString();
      } else {
         return input;
      }
   }

   public static void appendEscapingCData(final StringBuilder buf, final String str) {
      if (str != null) {
         int end = str.indexOf("]]>");
         if (end < 0) {
            buf.append(str);
         } else {
            int start;
            for (start = 0; end > -1; end = str.indexOf("]]>", start)) {
               buf.append(str.substring(start, end));
               buf.append("]]>]]&gt;<![CDATA[");
               start = end + CDATA_END_LEN;
               if (start >= str.length()) {
                  return;
               }
            }

            buf.append(str.substring(start));
         }
      }
   }

   public static String escapeJsonControlCharacters(final String input) {
      if (!Strings.isEmpty(input)
         && (
            input.indexOf(34) != -1
               || input.indexOf(92) != -1
               || input.indexOf(47) != -1
               || input.indexOf(8) != -1
               || input.indexOf(12) != -1
               || input.indexOf(10) != -1
               || input.indexOf(13) != -1
               || input.indexOf(9) != -1
         )) {
         StringBuilder buf = new StringBuilder(input.length() + 6);
         int len = input.length();

         for (int i = 0; i < len; i++) {
            char ch = input.charAt(i);
            String escBs = "\\";
            switch (ch) {
               case '\b':
                  buf.append("\\");
                  buf.append('b');
                  break;
               case '\t':
                  buf.append("\\");
                  buf.append('t');
                  break;
               case '\n':
                  buf.append("\\");
                  buf.append('n');
                  break;
               case '\f':
                  buf.append("\\");
                  buf.append('f');
                  break;
               case '\r':
                  buf.append("\\");
                  buf.append('r');
                  break;
               case '"':
                  buf.append("\\");
                  buf.append(ch);
                  break;
               case '/':
                  buf.append("\\");
                  buf.append(ch);
                  break;
               case '\\':
                  buf.append("\\");
                  buf.append(ch);
                  break;
               default:
                  buf.append(ch);
            }
         }

         return buf.toString();
      } else {
         return input;
      }
   }
}
