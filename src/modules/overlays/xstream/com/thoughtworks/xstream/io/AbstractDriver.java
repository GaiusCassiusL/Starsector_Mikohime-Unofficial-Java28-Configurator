package com.thoughtworks.xstream.io;

import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class AbstractDriver implements HierarchicalStreamDriver {
   private NameCoder replacer;

   public AbstractDriver() {
      this(new NoNameCoder());
   }

   public AbstractDriver(NameCoder nameCoder) {
      this.replacer = nameCoder;
   }

   protected NameCoder getNameCoder() {
      return this.replacer;
   }

   public HierarchicalStreamReader createReader(URL in) {
      InputStream stream = null;

      try {
         stream = in.openStream();
      } catch (IOException e) {
         throw new StreamException(e);
      }

      return this.createReader(stream);
   }

   public HierarchicalStreamReader createReader(File in) {
      try {
         return this.createReader(new FileInputStream(in));
      } catch (FileNotFoundException e) {
         throw new StreamException(e);
      }
   }
}
