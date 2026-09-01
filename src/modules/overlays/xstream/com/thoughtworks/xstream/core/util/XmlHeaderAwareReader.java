package com.thoughtworks.xstream.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public final class XmlHeaderAwareReader extends Reader {
   private final InputStreamReader reader;
   private final double version;
   private static final String KEY_ENCODING = "encoding";
   private static final String KEY_VERSION = "version";
   private static final String XML_TOKEN = "?xml";
   private static final int STATE_BOM = 0;
   private static final int STATE_START = 1;
   private static final int STATE_AWAIT_XML_HEADER = 2;
   private static final int STATE_ATTR_NAME = 3;
   private static final int STATE_ATTR_VALUE = 4;

   public XmlHeaderAwareReader(InputStream in) throws UnsupportedEncodingException, IOException {
      PushbackInputStream[] pin = new PushbackInputStream[]{in instanceof PushbackInputStream ? (PushbackInputStream)in : new PushbackInputStream(in, 64)};
      Map header = this.getHeader(pin);
      this.version = Double.parseDouble((String)header.get("version"));
      this.reader = new InputStreamReader(pin[0], (String)header.get("encoding"));
   }

   private Map getHeader(PushbackInputStream[] in) throws IOException {
      Map header = new HashMap();
      header.put("encoding", "UTF-8");
      header.put("version", "1.0");
      int state = 0;
      ByteArrayOutputStream out = new ByteArrayOutputStream(64);
      int i = 0;
      char ch = '\u0000';
      char valueEnd = 0;
      StringBuffer name = new StringBuffer();
      StringBuffer value = new StringBuffer();
      boolean escape = false;

      while (i != -1 && (i = in[0].read()) != -1) {
         out.write(i);
         ch = (char)i;
         switch (state) {
            case 0:
               if (ch == 239 && out.size() == 1 || ch == 187 && out.size() == 2 || ch == 191 && out.size() == 3) {
                  if (ch == 191) {
                     out.reset();
                     state = 1;
                  }
                  continue;
               }

               if (out.size() > 1) {
                  i = -1;
                  continue;
               }

               state = 1;
            case 1:
               break;
            case 2:
               if (!Character.isWhitespace(ch)) {
                  name.append(Character.toLowerCase(ch));
                  if (!"?xml".startsWith(name.substring(0))) {
                     i = -1;
                  }
               } else if (name.toString().equals("?xml")) {
                  state = 3;
                  name.setLength(0);
               } else {
                  i = -1;
               }
               continue;
            case 3:
               if (!Character.isWhitespace(ch)) {
                  if (ch == '=') {
                     state = 4;
                  } else {
                     ch = Character.toLowerCase(ch);
                     if (Character.isLetter(ch)) {
                        name.append(ch);
                     } else {
                        i = -1;
                     }
                  }
               } else if (name.length() > 0) {
                  i = -1;
               }
               continue;
            case 4:
               if (valueEnd == 0) {
                  if (ch != '"' && ch != '\'') {
                     i = -1;
                  } else {
                     valueEnd = ch;
                  }
               } else if (ch == '\\' && !escape) {
                  escape = true;
               } else if (ch == valueEnd && !escape) {
                  valueEnd = 0;
                  state = 3;
                  header.put(name.toString(), value.toString());
                  name.setLength(0);
                  value.setLength(0);
               } else {
                  escape = false;
                  if (ch != '\n') {
                     value.append(ch);
                  } else {
                     i = -1;
                  }
               }
            default:
               continue;
         }

         if (!Character.isWhitespace(ch)) {
            if (ch == '<') {
               state = 2;
            } else {
               i = -1;
            }
         }
      }

      byte[] pushbackData = out.toByteArray();
      i = pushbackData.length;

      while (i-- > 0) {
         byte b = pushbackData[i];

         try {
            in[0].unread(b);
         } catch (IOException ex) {
            in[0] = new PushbackInputStream(in[0], ++i);
         }
      }

      return header;
   }

   public String getEncoding() {
      return this.reader.getEncoding();
   }

   public double getVersion() {
      return this.version;
   }

   public void mark(int readAheadLimit) throws IOException {
      this.reader.mark(readAheadLimit);
   }

   public boolean markSupported() {
      return this.reader.markSupported();
   }

   public int read() throws IOException {
      return this.reader.read();
   }

   public int read(char[] cbuf, int offset, int length) throws IOException {
      return this.reader.read(cbuf, offset, length);
   }

   public int read(char[] cbuf) throws IOException {
      return this.reader.read(cbuf);
   }

   public boolean ready() throws IOException {
      return this.reader.ready();
   }

   public void reset() throws IOException {
      this.reader.reset();
   }

   public long skip(long n) throws IOException {
      return this.reader.skip(n);
   }

   public void close() throws IOException {
      this.reader.close();
   }

   public boolean equals(Object obj) {
      return this.reader.equals(obj);
   }

   public int hashCode() {
      return this.reader.hashCode();
   }

   public String toString() {
      return this.reader.toString();
   }
}
