package com.thoughtworks.xstream.io.binary;

import com.thoughtworks.xstream.io.AbstractDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class BinaryStreamDriver extends AbstractDriver {
   public HierarchicalStreamReader createReader(Reader in) {
      throw new UnsupportedOperationException("The BinaryDriver cannot use character-oriented input streams.");
   }

   public HierarchicalStreamReader createReader(InputStream in) {
      return new BinaryStreamReader(in);
   }

   public HierarchicalStreamWriter createWriter(Writer out) {
      throw new UnsupportedOperationException("The BinaryDriver cannot use character-oriented output streams.");
   }

   public HierarchicalStreamWriter createWriter(OutputStream out) {
      return new BinaryStreamWriter(out);
   }
}
