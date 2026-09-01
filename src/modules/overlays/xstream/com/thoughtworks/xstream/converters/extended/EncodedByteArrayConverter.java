package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.ByteConverter;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.StringCodec;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class EncodedByteArrayConverter implements Converter, SingleValueConverter {
   private static final ByteConverter byteConverter = new ByteConverter();
   private final StringCodec codec;

   public EncodedByteArrayConverter() {
      this(JVM.getBase64Codec());
   }

   public EncodedByteArrayConverter(StringCodec stringCodec) {
      this.codec = stringCodec;
   }

   public boolean canConvert(Class type) {
      return type != null && type.isArray() && type.getComponentType().equals(byte.class);
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      writer.setValue(this.toString(source));
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      String data = reader.getValue();
      return !reader.hasMoreChildren() ? this.fromString(data) : this.unmarshalIndividualByteElements(reader, context);
   }

   private Object unmarshalIndividualByteElements(HierarchicalStreamReader reader, UnmarshallingContext context) {
      List bytes = new ArrayList();

      for (boolean firstIteration = true; firstIteration || reader.hasMoreChildren(); firstIteration = false) {
         reader.moveDown();
         bytes.add(byteConverter.fromString(reader.getValue()));
         reader.moveUp();
      }

      byte[] result = new byte[bytes.size()];
      int i = 0;

      for (Byte b : bytes) {
         result[i] = b;
         i++;
      }

      return result;
   }

   public String toString(Object obj) {
      return this.codec.encode((byte[])obj);
   }

   public Object fromString(String str) {
      return this.codec.decode(str);
   }
}
