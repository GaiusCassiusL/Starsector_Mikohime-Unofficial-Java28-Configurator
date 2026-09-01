package com.thoughtworks.xstream.io.binary;

import com.thoughtworks.xstream.io.StreamException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class Token {
   private static final byte TYPE_MASK = 7;
   public static final byte TYPE_VERSION = 1;
   public static final byte TYPE_MAP_ID_TO_VALUE = 2;
   public static final byte TYPE_START_NODE = 3;
   public static final byte TYPE_END_NODE = 4;
   public static final byte TYPE_ATTRIBUTE = 5;
   public static final byte TYPE_VALUE = 6;
   private static final byte ID_MASK = 56;
   private static final byte ID_ONE_BYTE = 8;
   private static final byte ID_TWO_BYTES = 16;
   private static final byte ID_FOUR_BYTES = 24;
   private static final byte ID_EIGHT_BYTES = 32;
   private static final String ID_SPLITTED = "\u0000‡\u0000";
   private static final int MAX_UTF8_LENGTH = 65535;
   private final byte type;
   protected long id = -1L;
   protected String value;

   public Token(byte type) {
      this.type = type;
   }

   public byte getType() {
      return this.type;
   }

   public long getId() {
      return this.id;
   }

   public String getValue() {
      return this.value;
   }

   public String toString() {
      return this.getClass().getName() + " [id=" + this.id + ", value='" + this.value + "']";
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         Token token = (Token)o;
         if (this.id != token.id) {
            return false;
         } else if (this.type != token.type) {
            return false;
         } else {
            return this.value != null ? this.value.equals(token.value) : token.value == null;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.type;
      result = 29 * result + (int)(this.id ^ this.id >>> 32);
      return 29 * result + (this.value != null ? this.value.hashCode() : 0);
   }

   public abstract void writeTo(DataOutput var1, byte var2) throws IOException;

   public abstract void readFrom(DataInput var1, byte var2) throws IOException;

   protected void writeId(DataOutput out, long id, byte idType) throws IOException {
      if (id < 0L) {
         throw new IOException("id must not be negative " + id);
      }

      switch (idType) {
         case 8:
            out.writeByte((byte)id + -128);
            break;
         case 16:
            out.writeShort((short)id + -32768);
            break;
         case 24:
            out.writeInt((int)id + Integer.MIN_VALUE);
            break;
         case 32:
            out.writeLong(id + Long.MIN_VALUE);
            break;
         default:
            throw new Error("Unknown idType " + idType);
      }
   }

   protected void writeString(DataOutput out, String string) throws IOException {
      byte[] bytes = string.length() > 16383 ? string.getBytes("utf-8") : new byte[0];
      int length = bytes.length;
      if (length <= 65535) {
         out.writeUTF(string);
      } else {
         out.writeUTF("\u0000‡\u0000");
         out.writeInt(bytes.length);
         out.write(bytes);
      }
   }

   protected long readId(DataInput in, byte idType) throws IOException {
      switch (idType) {
         case 8:
            return in.readByte() - -128;
         case 16:
            return in.readShort() - -32768;
         case 24:
            return in.readInt() - Integer.MIN_VALUE;
         case 32:
            return in.readLong() - Long.MIN_VALUE;
         default:
            throw new Error("Unknown idType " + idType);
      }
   }

   protected String readString(DataInput in) throws IOException {
      String string = in.readUTF();
      if (!"\u0000‡\u0000".equals(string)) {
         return string;
      }

      int size = in.readInt();
      byte[] bytes = new byte[size];
      in.readFully(bytes);
      return new String(bytes, "utf-8");
   }

   public static class Attribute extends Token {
      public Attribute(long id, String value) {
         super((byte)5);
         this.id = id;
         this.value = value;
      }

      public Attribute() {
         super((byte)5);
      }

      public void writeTo(DataOutput out, byte idType) throws IOException {
         this.writeId(out, this.id, idType);
         this.writeString(out, this.value);
      }

      public void readFrom(DataInput in, byte idType) throws IOException {
         this.id = this.readId(in, idType);
         this.value = this.readString(in);
      }
   }

   public static class EndNode extends Token {
      public EndNode() {
         super((byte)4);
      }

      public void writeTo(DataOutput out, byte idType) {
      }

      public void readFrom(DataInput in, byte idType) {
      }
   }

   public static class Formatter {
      public void write(DataOutput out, Token token) throws IOException {
         long id = token.getId();
         byte idType;
         if (id <= 255L) {
            idType = 8;
         } else if (id <= 65535L) {
            idType = 16;
         } else if (id <= 4294967295L) {
            idType = 24;
         } else {
            idType = 32;
         }

         out.write(token.getType() + idType);
         token.writeTo(out, idType);
      }

      public Token read(DataInput in) throws IOException {
         byte nextByte = in.readByte();
         byte type = (byte)(nextByte & 7);
         byte idType = (byte)(nextByte & 56);
         Token token = this.contructToken(type);
         token.readFrom(in, idType);
         return token;
      }

      private Token contructToken(byte type) {
         switch (type) {
            case 2:
               return new Token.MapIdToValue();
            case 3:
               return new Token.StartNode();
            case 4:
               return new Token.EndNode();
            case 5:
               return new Token.Attribute();
            case 6:
               return new Token.Value();
            default:
               throw new StreamException("Unknown token type");
         }
      }
   }

   public static class MapIdToValue extends Token {
      public MapIdToValue(long id, String value) {
         super((byte)2);
         this.id = id;
         this.value = value;
      }

      public MapIdToValue() {
         super((byte)2);
      }

      public void writeTo(DataOutput out, byte idType) throws IOException {
         this.writeId(out, this.id, idType);
         this.writeString(out, this.value);
      }

      public void readFrom(DataInput in, byte idType) throws IOException {
         this.id = this.readId(in, idType);
         this.value = this.readString(in);
      }
   }

   public static class StartNode extends Token {
      public StartNode(long id) {
         super((byte)3);
         this.id = id;
      }

      public StartNode() {
         super((byte)3);
      }

      public void writeTo(DataOutput out, byte idType) throws IOException {
         this.writeId(out, this.id, idType);
      }

      public void readFrom(DataInput in, byte idType) throws IOException {
         this.id = this.readId(in, idType);
      }
   }

   public static class Value extends Token {
      public Value(String value) {
         super((byte)6);
         this.value = value;
      }

      public Value() {
         super((byte)6);
      }

      public void writeTo(DataOutput out, byte idType) throws IOException {
         this.writeString(out, this.value);
      }

      public void readFrom(DataInput in, byte idType) throws IOException {
         this.value = this.readString(in);
      }
   }
}
