package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.io.StreamException;
import java.io.IOException;
import java.io.Writer;

public class QuickWriter {
   private final Writer writer;
   private char[] buffer;
   private int pointer;

   public QuickWriter(Writer writer) {
      this(writer, 1024);
   }

   public QuickWriter(Writer writer, int bufferSize) {
      this.writer = writer;
      this.buffer = new char[bufferSize];
   }

   public void write(String str) {
      int len = str.length();
      if (this.pointer + len > this.buffer.length) {
         this.flush();
         if (len > this.buffer.length) {
            this.raw(str.toCharArray());
            return;
         }
      }

      str.getChars(0, len, this.buffer, this.pointer);
      this.pointer += len;
   }

   public void write(char c) {
      if (this.pointer + 1 > this.buffer.length) {
         this.flush();
         if (this.buffer.length == 0) {
            this.raw(c);
            return;
         }
      }

      this.buffer[this.pointer++] = c;
   }

   public void write(char[] c) {
      int len = c.length;
      if (this.pointer + len > this.buffer.length) {
         this.flush();
         if (len > this.buffer.length) {
            this.raw(c);
            return;
         }
      }

      System.arraycopy(c, 0, this.buffer, this.pointer, len);
      this.pointer += len;
   }

   public void flush() {
      try {
         this.writer.write(this.buffer, 0, this.pointer);
         this.pointer = 0;
         this.writer.flush();
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public void close() {
      try {
         this.writer.write(this.buffer, 0, this.pointer);
         this.pointer = 0;
         this.writer.close();
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   private void raw(char[] c) {
      try {
         this.writer.write(c);
         this.writer.flush();
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   private void raw(char c) {
      try {
         this.writer.write(c);
         this.writer.flush();
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }
}
