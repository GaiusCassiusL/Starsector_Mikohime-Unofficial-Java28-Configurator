package org.lwjgl.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class XPMFile {
   private byte[] bytes;
   private static final int WIDTH = 0;
   private static final int HEIGHT = 1;
   private static final int NUMBER_OF_COLORS = 2;
   private static final int CHARACTERS_PER_PIXEL = 3;
   private int[] format = new int[4];

   private XPMFile() {
   }

   public static XPMFile load(String file) throws IOException {
      try (InputStream input = new FileInputStream(new File(file))) {
         return load(input);
      }
   }

   public static XPMFile load(InputStream is) {
      XPMFile xFile = new XPMFile();
      xFile.readImage(is);
      return xFile;
   }

   public int getHeight() {
      return this.format[1];
   }

   public int getWidth() {
      return this.format[0];
   }

   public byte[] getBytes() {
      return this.bytes;
   }

   private void readImage(InputStream is) {
      LineNumberReader reader = new LineNumberReader(new InputStreamReader(is));

      try {
         HashMap<String, Integer> colors = new HashMap<>();
         this.format = parseFormat(nextLineOfInterest(reader));

         for (int i = 0; i < this.format[NUMBER_OF_COLORS]; i++) {
            Object[] colorDefinition = parseColor(nextLineOfInterest(reader), this.format[CHARACTERS_PER_PIXEL]);
            colors.put((String)colorDefinition[0], (Integer)colorDefinition[1]);
         }

         this.bytes = new byte[getByteCount(this.format)];

         for (int i = 0; i < this.format[HEIGHT]; i++) {
            this.parseImageLine(nextLineOfInterest(reader), this.format, colors, i);
         }
      } catch (IOException | RuntimeException e) {
         throw new IllegalArgumentException("Unable to parse XPM File", e);
      }
   }

   private static String nextLineOfInterest(LineNumberReader reader) throws IOException {
      String ret;
      while ((ret = reader.readLine()) != null) {
         if (ret.startsWith("\"")) {
            int endQuote = ret.lastIndexOf(34);
            if (endQuote <= 0) {
               throw new IllegalArgumentException("Malformed XPM line: " + ret);
            }

            return ret.substring(1, endQuote);
         }
      }

      throw new IOException("Unexpected end of XPM data");
   }

   private static int[] parseFormat(String formatLine) {
      StringTokenizer st = new StringTokenizer(formatLine);
      if (st.countTokens() < 4) {
         throw new IllegalArgumentException("Malformed XPM header: " + formatLine);
      }

      int width = Integer.parseInt(st.nextToken());
      int height = Integer.parseInt(st.nextToken());
      int numberOfColors = Integer.parseInt(st.nextToken());
      int charactersPerPixel = Integer.parseInt(st.nextToken());
      if (width <= 0 || height <= 0) {
         throw new IllegalArgumentException("Invalid XPM dimensions: " + width + "x" + height);
      }

      if (numberOfColors < 0) {
         throw new IllegalArgumentException("Invalid XPM color count: " + numberOfColors);
      }

      if (charactersPerPixel <= 0) {
         throw new IllegalArgumentException("Invalid XPM characters-per-pixel: " + charactersPerPixel);
      }

      int[] parsedFormat = new int[]{width, height, numberOfColors, charactersPerPixel};
      getByteCount(parsedFormat);
      getLineLength(parsedFormat);
      return parsedFormat;
   }

   private static int getByteCount(int[] format) {
      long byteCount = 4L * format[WIDTH] * format[HEIGHT];
      if (byteCount > Integer.MAX_VALUE) {
         throw new IllegalArgumentException("XPM image is too large: " + format[WIDTH] + "x" + format[HEIGHT]);
      }

      return (int)byteCount;
   }

   private static int getLineLength(int[] format) {
      long lineLength = 1L * format[WIDTH] * format[CHARACTERS_PER_PIXEL];
      if (lineLength > Integer.MAX_VALUE) {
         throw new IllegalArgumentException("XPM image row is too wide: " + format[WIDTH]);
      }

      return (int)lineLength;
   }

   private static Object[] parseColor(String line, int charactersPerPixel) {
      if (line.length() < charactersPerPixel) {
         throw new IllegalArgumentException("Malformed XPM color definition: " + line);
      }

      String key = line.substring(0, charactersPerPixel);
      StringTokenizer st = new StringTokenizer(line.substring(charactersPerPixel).trim());
      if (!st.hasMoreTokens()) {
         throw new IllegalArgumentException("Missing XPM color type: " + line);
      }

      st.nextToken();
      if (!st.hasMoreTokens()) {
         throw new IllegalArgumentException("Missing XPM color value: " + line);
      }

      String color = st.nextToken();
      if (color.startsWith("#")) {
         color = color.substring(1);
      }

      return new Object[]{key, Integer.parseInt(color, 16)};
   }

   private void parseImageLine(String line, int[] format, Map<String, Integer> colors, int index) {
      int expectedLineLength = getLineLength(format);
      if (line.length() < expectedLineLength) {
         throw new IllegalArgumentException("XPM image row is shorter than expected");
      }

      int offset = index * 4 * format[WIDTH];

      for (int i = 0; i < format[WIDTH]; i++) {
         String key = line.substring(i * format[CHARACTERS_PER_PIXEL], i * format[CHARACTERS_PER_PIXEL] + format[CHARACTERS_PER_PIXEL]);
         Integer color = colors.get(key);
         if (color == null) {
            throw new IllegalArgumentException("Unknown XPM color key: " + key);
         }

         this.bytes[offset + i * 4] = (byte)((color & 0xFF0000) >> 16);
         this.bytes[offset + i * 4 + 1] = (byte)((color & 0xFF00) >> 8);
         this.bytes[offset + i * 4 + 2] = (byte)((color & 0xFF) >> 0);
         this.bytes[offset + i * 4 + 3] = -1;
      }
   }

   public static void main(String[] args) {
      if (args.length != 1) {
         System.out.println("usage:\nXPMFile <file>");
         return;
      }

      try {
         String out = args[0].substring(0, args[0].indexOf(".")) + ".raw";
         XPMFile file = load(args[0]);
         try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(out)))) {
            bos.write(file.getBytes());
         }
      } catch (Exception e) {
         System.err.println("Failed to convert XPM file: " + e.getMessage());
      }
   }
}
