package org.apache.logging.log4j.core.util;

import java.nio.charset.Charset;

public final class StringEncoder {
   private StringEncoder() {
   }

   public static byte[] toBytes(final String str, final Charset charset) {
      return str != null ? str.getBytes(charset != null ? charset : Charset.defaultCharset()) : null;
   }

   @Deprecated
   public static byte[] encodeSingleByteChars(final CharSequence s) {
      int length = s.length();
      byte[] result = new byte[length];
      encodeString(s, 0, length, result);
      return result;
   }

   @Deprecated
   public static int encodeIsoChars(final CharSequence charArray, int charIndex, final byte[] byteArray, int byteIndex, final int length) {
      int i;
      for (i = 0; i < length; i++) {
         char c = charArray.charAt(charIndex++);
         if (c > 255) {
            break;
         }

         byteArray[byteIndex++] = (byte)c;
      }

      return i;
   }

   @Deprecated
   public static int encodeString(final CharSequence charArray, final int charOffset, final int charLength, final byte[] byteArray) {
      int byteOffset = 0;
      int length = Math.min(charLength, byteArray.length);
      int charDoneIndex = charOffset + length;
      int currentCharOffset = charOffset;
      int currentCharLength = charLength;

      while (currentCharOffset < charDoneIndex) {
         int done = encodeIsoChars(charArray, currentCharOffset, byteArray, byteOffset, length);
         currentCharOffset += done;
         byteOffset += done;
         if (done != length) {
            char c = charArray.charAt(currentCharOffset++);
            if (Character.isHighSurrogate(c) && currentCharOffset < charDoneIndex && Character.isLowSurrogate(charArray.charAt(currentCharOffset))) {
               if (currentCharLength > byteArray.length) {
                  charDoneIndex++;
                  currentCharLength--;
               }

               currentCharOffset++;
            }

            byteArray[byteOffset++] = 63;
            length = Math.min(charDoneIndex - currentCharOffset, byteArray.length - byteOffset);
         }
      }

      return byteOffset;
   }
}
