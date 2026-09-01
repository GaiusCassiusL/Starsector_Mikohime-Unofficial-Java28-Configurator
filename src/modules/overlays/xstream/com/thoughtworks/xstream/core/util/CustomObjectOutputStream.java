package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.io.StreamException;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.util.Map;

public class CustomObjectOutputStream extends ObjectOutputStream {
   private FastStack callbacks = new FastStack(1);
   private FastStack customFields = new FastStack(1);
   private static final String DATA_HOLDER_KEY = CustomObjectOutputStream.class.getName();

   public static synchronized CustomObjectOutputStream getInstance(DataHolder whereFrom, CustomObjectOutputStream.StreamCallback callback) {
      try {
         CustomObjectOutputStream result = (CustomObjectOutputStream)whereFrom.get(DATA_HOLDER_KEY);
         if (result == null) {
            result = new CustomObjectOutputStream(callback);
            whereFrom.put(DATA_HOLDER_KEY, result);
         } else {
            result.pushCallback(callback);
         }

         return result;
      } catch (SecurityException e) {
         throw new ObjectAccessException("Cannot create CustomObjectStream", e);
      } catch (IOException e) {
         throw new StreamException("Cannot create CustomObjectStream", e);
      }
   }

   public CustomObjectOutputStream(CustomObjectOutputStream.StreamCallback callback) throws IOException, SecurityException {
      this.callbacks.push(callback);
   }

   public void pushCallback(CustomObjectOutputStream.StreamCallback callback) {
      this.callbacks.push(callback);
   }

   public CustomObjectOutputStream.StreamCallback popCallback() {
      return (CustomObjectOutputStream.StreamCallback)this.callbacks.pop();
   }

   public CustomObjectOutputStream.StreamCallback peekCallback() {
      return (CustomObjectOutputStream.StreamCallback)this.callbacks.peek();
   }

   public void defaultWriteObject() throws IOException {
      this.peekCallback().defaultWriteObject();
   }

   protected void writeObjectOverride(Object obj) throws IOException {
      this.peekCallback().writeToStream(obj);
   }

   public void writeBoolean(boolean val) throws IOException {
      this.peekCallback().writeToStream(val ? Boolean.TRUE : Boolean.FALSE);
   }

   public void writeByte(int val) throws IOException {
      this.peekCallback().writeToStream(new Byte((byte)val));
   }

   public void writeInt(int val) throws IOException {
      this.peekCallback().writeToStream(new Integer(val));
   }

   public void writeChar(int val) throws IOException {
      this.peekCallback().writeToStream(new Character((char)val));
   }

   public void writeDouble(double val) throws IOException {
      this.peekCallback().writeToStream(new Double(val));
   }

   public void writeFloat(float val) throws IOException {
      this.peekCallback().writeToStream(new Float(val));
   }

   public void writeLong(long val) throws IOException {
      this.peekCallback().writeToStream(new Long(val));
   }

   public void writeShort(int val) throws IOException {
      this.peekCallback().writeToStream(new Short((short)val));
   }

   public void write(byte[] buf) throws IOException {
      this.peekCallback().writeToStream(buf);
   }

   public void writeChars(String str) throws IOException {
      this.peekCallback().writeToStream(str.toCharArray());
   }

   public void writeUTF(String str) throws IOException {
      this.peekCallback().writeToStream(str);
   }

   public void write(int val) throws IOException {
      this.peekCallback().writeToStream(new Byte((byte)val));
   }

   public void write(byte[] buf, int off, int len) throws IOException {
      byte[] b = new byte[len];
      System.arraycopy(buf, off, b, 0, len);
      this.peekCallback().writeToStream(b);
   }

   public void flush() throws IOException {
      this.peekCallback().flush();
   }

   public void close() throws IOException {
      this.peekCallback().close();
   }

   public PutField putFields() {
      CustomObjectOutputStream.CustomPutField result = new CustomObjectOutputStream.CustomPutField(null);
      this.customFields.push(result);
      return result;
   }

   public void writeFields() throws IOException {
      CustomObjectOutputStream.CustomPutField customPutField = (CustomObjectOutputStream.CustomPutField)this.customFields.pop();
      this.peekCallback().writeFieldsToStream(customPutField.asMap());
   }

   public void reset() {
      throw new UnsupportedOperationException();
   }

   public void useProtocolVersion(int version) {
      throw new UnsupportedOperationException();
   }

   public void writeBytes(String str) {
      throw new UnsupportedOperationException();
   }

   public void writeUnshared(Object obj) {
      throw new UnsupportedOperationException();
   }

   private class CustomPutField extends PutField {
      private final Map fields = new OrderRetainingMap();

      private CustomPutField() {
      }

      public Map asMap() {
         return this.fields;
      }

      public void write(ObjectOutput out) throws IOException {
         CustomObjectOutputStream.this.peekCallback().writeToStream(this.asMap());
      }

      public void put(String name, Object val) {
         this.fields.put(name, val);
      }

      public void put(String name, byte val) {
         this.put(name, new Byte(val));
      }

      public void put(String name, char val) {
         this.put(name, new Character(val));
      }

      public void put(String name, double val) {
         this.put(name, new Double(val));
      }

      public void put(String name, float val) {
         this.put(name, new Float(val));
      }

      public void put(String name, int val) {
         this.put(name, new Integer(val));
      }

      public void put(String name, long val) {
         this.put(name, new Long(val));
      }

      public void put(String name, short val) {
         this.put(name, new Short(val));
      }

      public void put(String name, boolean val) {
         this.put(name, val ? Boolean.TRUE : Boolean.FALSE);
      }
   }

   public interface StreamCallback {
      void writeToStream(Object var1) throws IOException;

      void writeFieldsToStream(Map var1) throws IOException;

      void defaultWriteObject() throws IOException;

      void flush() throws IOException;

      void close() throws IOException;
   }
}
