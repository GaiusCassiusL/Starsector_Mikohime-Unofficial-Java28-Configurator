package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.core.StringCodec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

public class Base64JavaUtilCodec implements StringCodec {
   private final Decoder decoder;
   private final Encoder encoder;

   public Base64JavaUtilCodec() {
      this(Base64.getEncoder(), Base64.getMimeDecoder());
   }

   public Base64JavaUtilCodec(Encoder encoder, Decoder decoder) {
      this.encoder = encoder;
      this.decoder = decoder;
   }

   @Override
   public byte[] decode(String base64) {
      return this.decoder.decode(base64);
   }

   @Override
   public String encode(byte[] data) {
      return this.encoder.encodeToString(data);
   }
}
