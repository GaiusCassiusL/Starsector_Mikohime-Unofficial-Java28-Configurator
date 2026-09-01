package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.core.StringCodec;
import javax.xml.bind.DatatypeConverter;

public class Base64JAXBCodec implements StringCodec {
   @Override
   public byte[] decode(String base64) {
      return DatatypeConverter.parseBase64Binary(base64);
   }

   @Override
   public String encode(byte[] data) {
      return DatatypeConverter.printBase64Binary(data);
   }
}
