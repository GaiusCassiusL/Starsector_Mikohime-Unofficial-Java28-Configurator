package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.core.StringCodec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class Base64Encoder implements StringCodec {
   private static final char[] SIXTY_FOUR_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
   private static final int[] REVERSE_MAPPING = new int[123];
   private final boolean lineBreaks;

   public Base64Encoder() {
      this(false);
   }

   public Base64Encoder(boolean lineBreaks) {
      this.lineBreaks = lineBreaks;
   }

   public String encode(byte[] input) {
      int stringSize = this.computeResultingStringSize(input);
      StringBuffer result = new StringBuffer(stringSize);
      int outputCharCount = 0;

      for (int i = 0; i < input.length; i += 3) {
         int remaining = Math.min(3, input.length - i);
         int oneBigNumber = (input[i] & 255) << 16 | (remaining <= 1 ? 0 : input[i + 1] & 0xFF) << 8 | (remaining <= 2 ? 0 : input[i + 2] & 0xFF);

         for (int j = 0; j < 4; j++) {
            result.append(remaining + 1 > j ? SIXTY_FOUR_CHARS[63 & oneBigNumber >> 6 * (3 - j)] : '=');
         }

         if (this.lineBreaks) {
            outputCharCount += 4;
            if (outputCharCount % 76 == 0 && i + 3 < input.length) {
               result.append('\n');
            }
         }
      }

      return result.toString();
   }

   int computeResultingStringSize(byte[] input) {
      int stringSize = input.length / 3 + (input.length % 3 == 0 ? 0 : 1);
      stringSize *= 4;
      if (this.lineBreaks) {
         stringSize += stringSize / 76;
      }

      return stringSize;
   }

   public byte[] decode(String input) {
      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         StringReader in = new StringReader(input);

         for (int i = 0; i < input.length(); i += 4) {
            int[] a = new int[]{this.mapCharToInt(in), this.mapCharToInt(in), this.mapCharToInt(in), this.mapCharToInt(in)};
            int oneBigNumber = (a[0] & 63) << 18 | (a[1] & 63) << 12 | (a[2] & 63) << 6 | a[3] & 63;

            for (int j = 0; j < 3; j++) {
               if (a[j + 1] >= 0) {
                  out.write(0xFF & oneBigNumber >> 8 * (2 - j));
               }
            }
         }

         return out.toByteArray();
      } catch (IOException e) {
         throw new Error(e + ": " + e.getMessage());
      }
   }

   private int mapCharToInt(Reader input) throws IOException {
      int c;
      while ((c = input.read()) != -1) {
         int result = REVERSE_MAPPING[c];
         if (result != 0) {
            return result - 1;
         }

         if (c == 61) {
            return -1;
         }
      }

      return -1;
   }

   static {
      for (int i = 0; i < SIXTY_FOUR_CHARS.length; i++) {
         REVERSE_MAPPING[SIXTY_FOUR_CHARS[i]] = i + 1;
      }
   }
}
